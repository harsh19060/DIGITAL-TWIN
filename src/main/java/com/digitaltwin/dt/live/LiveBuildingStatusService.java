package com.digitaltwin.dt.live;

import com.digitaltwin.dt.bookings.BookingStatus;
import com.digitaltwin.dt.bookings.RoomBooking;
import com.digitaltwin.dt.bookings.RoomBookingRepository;
import com.digitaltwin.dt.rooms.Room;
import com.digitaltwin.dt.rooms.RoomRepository;
import com.digitaltwin.dt.teachers.Teacher;
import com.digitaltwin.dt.teachers.TeacherRepository;
import com.digitaltwin.dt.timetable.DatedTimetableService;
import com.digitaltwin.dt.timetable.DatedTimetableService.DatedClass;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiveBuildingStatusService {
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final RoomBookingRepository bookingRepository;
    private final DatedTimetableService datedTimetableService;

    public LiveBuildingStatusService(RoomRepository roomRepository, TeacherRepository teacherRepository,
                                     RoomBookingRepository bookingRepository,
                                     DatedTimetableService datedTimetableService) {
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.bookingRepository = bookingRepository;
        this.datedTimetableService = datedTimetableService;
    }

    @Transactional(readOnly = true)
    public TeacherCurrentLocation teacherNow(String staffCode, LocalDateTime requestedAt) {
        LocalDateTime at = requestedAt == null ? LocalDateTime.now() : requestedAt;
        Teacher teacher = teacherRepository.findByStaffCodeIgnoreCase(staffCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found: " + staffCode));
        List<LiveClass> classes = classesOn(at);
        List<LiveClass> teacherClasses = classes.stream()
                .filter(value -> value.teacherCode().equalsIgnoreCase(teacher.getStaffCode()))
                .sorted(Comparator.comparing(LiveClass::startTime)).toList();
        LiveClass current = teacherClasses.stream().filter(value -> isInProgress(value, at.toLocalTime())).findFirst().orElse(null);
        LiveClass next = teacherClasses.stream().filter(value -> value.startTime().isAfter(at.toLocalTime())).findFirst().orElse(null);
        return new TeacherCurrentLocation(teacher.getStaffCode(), teacher.getFullName(),
                current == null ? "NO_CLASS_SCHEDULED" : "IN_CLASS", current, next,
                at.atZone(java.time.ZoneId.systemDefault()).toInstant(), "TIMETABLE_AND_CONFIRMED_BOOKINGS");
    }

    @Transactional(readOnly = true)
    public List<RoomCurrentStatus> roomsNow(LocalDateTime requestedAt) {
        LocalDateTime at = requestedAt == null ? LocalDateTime.now() : requestedAt;
        Map<String, LiveClass> activeByRoom = classesOn(at).stream()
                .filter(value -> isInProgress(value, at.toLocalTime()))
                .collect(Collectors.toMap(value -> value.roomNumber().toLowerCase(Locale.ROOT), Function.identity(),
                        (first, ignored) -> first));
        Instant observedAt = at.atZone(java.time.ZoneId.systemDefault()).toInstant();
        return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "roomNumber")).stream()
                .map(room -> roomStatus(room, activeByRoom.get(room.getRoomNumber().toLowerCase(Locale.ROOT)), observedAt))
                .toList();
    }

    private List<LiveClass> classesOn(LocalDateTime at) {
        List<LiveClass> classes = new ArrayList<>();
        datedTimetableService.onDate(at.toLocalDate()).classes().stream()
                .filter(entry -> "SCHEDULED".equals(entry.status()))
                .map(this::fromTimetable).forEach(classes::add);
        bookingRepository.findAllByClassDateAndStatusOrderByStartTimeAsc(at.toLocalDate(), BookingStatus.CONFIRMED)
                .stream().map(this::fromBooking).forEach(classes::add);
        return classes;
    }

    private LiveClass fromTimetable(DatedClass entry) {
        return new LiveClass(entry.subjectCode(), entry.subjectName(), entry.sectionCode(),
                entry.teacherCode(), entry.teacherName(), entry.roomNumber(), entry.startTime(), entry.endTime(), "TIMETABLE");
    }

    private LiveClass fromBooking(RoomBooking booking) {
        return new LiveClass(booking.getSubject().getCode(), booking.getSubject().getName(),
                booking.getSection().getCode(), booking.getTeacher().getStaffCode(),
                booking.getTeacher().getFullName(), booking.getRoom().getRoomNumber(),
                booking.getStartTime(), booking.getEndTime(), "EXTRA_BOOKING");
    }

    private RoomCurrentStatus roomStatus(Room room, LiveClass current, Instant observedAt) {
        return new RoomCurrentStatus(room.getRoomNumber(), room.getFloor(), room.getCapacity(), room.getRoomType(),
                current == null ? "NO_CLASS_SCHEDULED" : "OCCUPIED", current, observedAt,
                "TIMETABLE_AND_CONFIRMED_BOOKINGS");
    }

    private boolean isInProgress(LiveClass value, LocalTime time) {
        return !time.isBefore(value.startTime()) && time.isBefore(value.endTime());
    }
}
