package com.digitaltwin.dt.bookings;

import com.digitaltwin.dt.notifications.SectionNotification;
import com.digitaltwin.dt.notifications.SectionNotificationRepository;
import com.digitaltwin.dt.rooms.Room;
import com.digitaltwin.dt.rooms.RoomRepository;
import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.sections.StudentSection;
import com.digitaltwin.dt.subjects.Subject;
import com.digitaltwin.dt.subjects.SubjectRepository;
import com.digitaltwin.dt.teachers.Teacher;
import com.digitaltwin.dt.teachers.TeacherRepository;
import com.digitaltwin.dt.timetable.DatedTimetableService;
import com.digitaltwin.dt.timetable.TeacherCancellationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RoomBookingService {
    private static final LocalTime OPEN_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(19, 0);

    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final RoomBookingRepository bookingRepository;
    private final SectionNotificationRepository notificationRepository;
    private final DatedTimetableService datedTimetableService;

    public RoomBookingService(RoomRepository roomRepository, TeacherRepository teacherRepository,
                              SectionRepository sectionRepository, SubjectRepository subjectRepository,
                              RoomBookingRepository bookingRepository,
                              SectionNotificationRepository notificationRepository,
                              DatedTimetableService datedTimetableService) {
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
        this.datedTimetableService = datedTimetableService;
    }

    @Transactional(readOnly = true)
    public BookingPreview preview(BookingRequest request) {
        validateTime(request.startTime(), request.endTime());
        ResolvedBooking resolved = resolve(request);
        String conflict = findConflict(resolved);
        return new BookingPreview(conflict == null, conflict == null ? "Available" : conflict,
                request.roomNumber(), request.classDate(), request.startTime(), request.endTime());
    }

    @Transactional
    public BookingResponse confirm(BookingRequest request) {
        validateTime(request.startTime(), request.endTime());
        ResolvedBooking resolved = resolve(request);
        String conflict = findConflict(resolved);
        if (conflict != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, conflict);
        }

        RoomBooking booking = bookingRepository.save(new RoomBooking(resolved.room, resolved.teacher,
                resolved.section, resolved.subject, request.classDate(), request.startTime(), request.endTime()));
        notificationRepository.save(new SectionNotification(resolved.section, request.classDate(),
                "Extra class scheduled: " + resolved.subject.getCode(),
                String.format("%s scheduled %s in %s from %s to %s.", resolved.teacher.getFullName(),
                        resolved.subject.getName(), resolved.room.getRoomNumber(), request.startTime(), request.endTime())));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForDate(LocalDate date) {
        return bookingRepository.findAllByClassDateOrderByStartTimeAsc(date)
                .stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String reason) {
        RoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking);
        }
        booking.cancel(reason.trim());
        notificationRepository.save(new SectionNotification(booking.getSection(), booking.getClassDate(),
                "Extra class cancelled: " + booking.getSubject().getCode(),
                String.format("The extra class for %s in %s from %s to %s was cancelled. Reason: %s.",
                        booking.getSubject().getName(), booking.getRoom().getRoomNumber(),
                        booking.getStartTime(), booking.getEndTime(), reason.trim())));
        return BookingResponse.from(booking);
    }

    private String findConflict(ResolvedBooking request) {
        var schedule = datedTimetableService.onDate(request.request.classDate());
        for (var entry : schedule.classes()) {
            if (!"SCHEDULED".equals(entry.status())
                    || !overlaps(entry.startTime(), entry.endTime(), request.request.startTime(), request.request.endTime())) {
                continue;
            }
            if (entry.roomNumber().equalsIgnoreCase(request.room.getRoomNumber())) {
                return "Room " + entry.roomNumber() + " is occupied by " + entry.subjectCode() + " during that time";
            }
            if (entry.teacherCode().equalsIgnoreCase(request.teacher.getStaffCode())) {
                return "Teacher " + request.teacher.getStaffCode() + " has a class during that time";
            }
            if (entry.sectionCode().equalsIgnoreCase(request.section.getCode())) {
                return "Section " + request.section.getCode() + " has a class during that time";
            }
        }

        for (RoomBooking booking : bookingRepository.findAllByClassDateAndStatusOrderByStartTimeAsc(
                request.request.classDate(), BookingStatus.CONFIRMED)) {
            if (!overlaps(booking.getStartTime(), booking.getEndTime(),
                    request.request.startTime(), request.request.endTime())) {
                continue;
            }
            if (booking.getRoom().getId().equals(request.room.getId())) return "Room already has a confirmed booking";
            if (booking.getTeacher().getId().equals(request.teacher.getId())) return "Teacher already has a confirmed booking";
            if (booking.getSection().getId().equals(request.section.getId())) return "Section already has a confirmed booking";
        }
        return null;
    }

    private ResolvedBooking resolve(BookingRequest request) {
        Room room = roomRepository.findByRoomNumberIgnoreCase(request.roomNumber().trim())
                .orElseThrow(() -> notFound("Room", request.roomNumber()));
        Teacher teacher = teacherRepository.findByStaffCodeIgnoreCase(request.teacherCode().trim())
                .orElseThrow(() -> notFound("Teacher", request.teacherCode()));
        StudentSection section = sectionRepository.findByCodeIgnoreCase(request.sectionCode().trim())
                .orElseThrow(() -> notFound("Section", request.sectionCode()));
        Subject subject = subjectRepository.findByCodeIgnoreCase(request.subjectCode().trim())
                .orElseThrow(() -> notFound("Subject", request.subjectCode()));
        return new ResolvedBooking(request, room, teacher, section, subject);
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)
                || startTime.isBefore(OPEN_TIME) || endTime.isAfter(CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking time must be within 07:00–19:00 and end after start");
        }
    }

    private boolean overlaps(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private ResponseStatusException notFound(String type, String identifier) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found: " + identifier);
    }

    private record ResolvedBooking(BookingRequest request, Room room, Teacher teacher,
                                   StudentSection section, Subject subject) { }

    public record BookingRequest(@NotBlank @Size(max = 30) String roomNumber,
                                 @NotBlank @Size(max = 40) String teacherCode,
                                 @NotBlank @Size(max = 40) String sectionCode,
                                 @NotBlank @Size(max = 40) String subjectCode,
                                 @NotNull LocalDate classDate,
                                 @NotNull LocalTime startTime,
                                 @NotNull LocalTime endTime) { }

    public record BookingPreview(boolean available, String message, String roomNumber,
                                 LocalDate classDate, LocalTime startTime, LocalTime endTime) { }
}

