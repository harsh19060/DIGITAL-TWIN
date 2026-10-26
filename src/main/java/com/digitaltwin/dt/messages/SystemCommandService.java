package com.digitaltwin.dt.messages;

import com.digitaltwin.dt.bookings.RoomBookingService;
import com.digitaltwin.dt.live.LiveBuildingStatusService;
import com.digitaltwin.dt.rooms.RoomRepository;
import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.session.CampusIdentityService;
import com.digitaltwin.dt.teachers.TeacherRepository;
import com.digitaltwin.dt.timetable.TeacherCancellationService;
import com.digitaltwin.dt.timetable.TimetableEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SystemCommandService {
    private static final Pattern BOOKING_COMMAND = Pattern.compile(
            "(?i).*schedule\\s+(?:an?\\s+)?extra\\s+class\\s+for\\s+([a-z0-9_-]+)\\s+(?:subject\\s+)?([a-z0-9_-]+)\\s+on\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{1,2}:\\d{2})\\s*(?:-|to)\\s*(\\d{1,2}:\\d{2}).*");
    private static final Pattern SECTION_ONLY_COMMAND = Pattern.compile(
            "(?i).*schedule\\s+(?:an?\\s+)?extra\\s+class\\s+for\\s+([a-z0-9_-]+).*");
    private final CampusIdentityService identityService;
    private final CampusMessageService messageService;
    private final LiveBuildingStatusService liveStatusService;
    private final TeacherCancellationService cancellationService;
    private final RoomBookingService bookingService;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;
    private final TimetableEntryRepository timetableRepository;
    private final Map<String, RoomBookingService.BookingRequest> pendingBookings = new ConcurrentHashMap<>();

    public SystemCommandService(CampusIdentityService identityService, CampusMessageService messageService,
                                LiveBuildingStatusService liveStatusService,
                                TeacherCancellationService cancellationService,
                                RoomBookingService bookingService, RoomRepository roomRepository,
                                SectionRepository sectionRepository, TimetableEntryRepository timetableRepository) {
        this.identityService = identityService;
        this.messageService = messageService;
        this.liveStatusService = liveStatusService;
        this.cancellationService = cancellationService;
        this.bookingService = bookingService;
        this.roomRepository = roomRepository;
        this.sectionRepository = sectionRepository;
        this.timetableRepository = timetableRepository;
    }

    public Result execute(org.springframework.security.core.Authentication authentication, String command) {
        var identity = identityService.requireParticipant(authentication);
        String text = command == null ? "" : command.trim();
        if (text.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type a system request.");
        String reply;
        try {
            reply = respond(identity, text);
        } catch (ResponseStatusException ex) {
            reply = ex.getReason() == null ? "I couldn't complete that request." : ex.getReason();
        } catch (IllegalArgumentException ex) {
            reply = "I couldn't understand those details. Use the format shown in the help message.";
        }
        CampusMessageResponse incoming = messageService.sendSystemReply(identity.participantType(),
                identity.participantCode(), text, reply);
        return new Result(incoming, reply);
    }

    private String respond(CampusIdentityService.Identity identity, String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.equals("help") || normalized.equals("what can you do")) {
            return "I can find free classrooms, cancel a teacher's classes for today, and schedule an extra class. " +
                    "For a specific time, send: schedule extra class for A7 DPD on 2026-10-27 14:00-16:00. " +
                    "Or just ask me to schedule an extra class for A7 and I will suggest a slot.";
        }
        if (normalized.matches("^(confirm|yes|confirm booking)$")) return confirmBooking(identity);
        if (normalized.contains("free") || normalized.contains("empty") || normalized.contains("available")) {
            var rooms = liveStatusService.roomsNow(LocalDateTime.now()).stream()
                    .filter(room -> room.roomType().toLowerCase(Locale.ROOT).contains("classroom"))
                    .filter(room -> "NO_CLASS_SCHEDULED".equals(room.status()))
                    .map(room -> room.roomNumber() + " (floor " + room.floor() + ")").toList();
            return rooms.isEmpty() ? "No classrooms are free according to the timetable right now."
                    : "Classrooms with no class scheduled right now: " + String.join(", ", rooms) +
                    ". This is timetable-based and doesn't confirm physical occupancy.";
        }
        if (normalized.contains("cancel") && normalized.contains("class")) {
            if (!"TEACHER".equals(identity.role())) {
                return "Only a teacher can cancel their own classes. You are signed in as a student.";
            }
            var result = cancellationService.cancelClasses(identity.participantCode(),
                    new TeacherCancellationService.CancellationRequest(LocalDate.now(), "Teacher reported absence"));
            for (String section : result.notifiedSections()) {
                String affectedClasses = result.cancelledClasses().stream()
                        .filter(item -> item.sectionCode().equalsIgnoreCase(section))
                        .map(item -> item.subjectCode() + " " + item.startTime() + "-" + item.endTime())
                        .collect(java.util.stream.Collectors.joining(", "));
                messageService.sendSectionSystemNotice(section, identity.participantCode() +
                        " cancelled today's classes due to absence: " + affectedClasses + ".");
            }
            return "Cancelled " + result.cancelledClassCount() + " class(es) for today. Sections notified: " +
                    (result.notifiedSections().isEmpty() ? "none" : String.join(", ", result.notifiedSections())) + ".";
        }
        Matcher booking = BOOKING_COMMAND.matcher(text);
        if (booking.matches()) return previewBooking(identity, booking);
        Matcher sectionOnly = SECTION_ONLY_COMMAND.matcher(text);
        if (sectionOnly.matches()) return suggestBooking(identity, sectionOnly.group(1));
        return "I didn't recognize that request. Try 'free classrooms', 'cancel all my classes today', or 'help'.";
    }

    private String previewBooking(CampusIdentityService.Identity identity, Matcher booking) {
        if (!"TEACHER".equals(identity.role())) return "Only a teacher can schedule an extra class.";
        String section = booking.group(1);
        String subject = booking.group(2);
        LocalDate date = LocalDate.parse(booking.group(3));
        LocalTime start = LocalTime.parse(booking.group(4));
        LocalTime end = LocalTime.parse(booking.group(5));
        for (var room : bookableRooms()) {
            var request = new RoomBookingService.BookingRequest(room.getRoomNumber(), identity.participantCode(),
                    section, subject, date, start, end);
            try {
                var preview = bookingService.preview(request);
                if (preview.available()) {
                    pendingBookings.put(identity.participantCode().toLowerCase(Locale.ROOT), request);
                    return "Available: " + date + " " + start + "–" + end + " in " + room.getRoomNumber() +
                            " for " + section + " (" + subject + "). Reply CONFIRM to book it, or send another time.";
                }
            } catch (ResponseStatusException ignored) {
                // Try the next bookable room when capacity or class assignment does not fit.
            }
        }
        return "No room is available for " + section + " (" + subject + ") at that time. Try another slot.";
    }

    private String suggestBooking(CampusIdentityService.Identity identity, String sectionCode) {
        if (!"TEACHER".equals(identity.role())) return "Only a teacher can schedule an extra class.";
        var section = sectionRepository.findByCodeIgnoreCase(sectionCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found: " + sectionCode));
        var subjects = timetableRepository
                .findAllByTeacher_StaffCodeIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(identity.participantCode()).stream()
                .filter(entry -> entry.getSection().getCode().equalsIgnoreCase(section.getCode()))
                .map(entry -> entry.getSubject().getCode()).distinct().toList();
        if (subjects.isEmpty()) {
            return "I couldn't find a subject you teach in " + section.getCode() +
                    ". Include a subject, date, and time, or ask the campus admin to check the timetable.";
        }

        var rooms = bookableRooms();
        for (int offset = 1; offset <= 7; offset++) {
            LocalDate date = LocalDate.now().plusDays(offset);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (int hour = 9; hour < 18; hour++) {
                LocalTime start = LocalTime.of(hour, 0);
                LocalTime end = start.plusHours(1);
                for (String subject : subjects) {
                    for (var room : rooms) {
                        var request = new RoomBookingService.BookingRequest(room.getRoomNumber(),
                                identity.participantCode(), section.getCode(), subject, date, start, end);
                        try {
                            if (bookingService.preview(request).available()) {
                                pendingBookings.put(identity.participantCode().toLowerCase(Locale.ROOT), request);
                                return "I found a free slot for " + section.getCode() + ": " + date + " " +
                                        start + "–" + end + " in " + room.getRoomNumber() + " for " + subject +
                                        ". Reply CONFIRM to book it, or ask for another slot.";
                            }
                        } catch (ResponseStatusException ignored) {
                            // Skip rooms that cannot fit this class or match the requested subject.
                        }
                    }
                }
            }
        }
        return "I couldn't find an open one-hour slot for " + section.getCode() +
                " during the next seven days. Try specifying a different date or time.";
    }

    private java.util.List<com.digitaltwin.dt.rooms.Room> bookableRooms() {
        return roomRepository.findAll().stream()
                .filter(room -> room.getRoomType().toLowerCase(Locale.ROOT).contains("classroom")
                        || room.getRoomType().toLowerCase(Locale.ROOT).contains("lab"))
                .sorted(java.util.Comparator.comparing(room -> room.getRoomNumber().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private String confirmBooking(CampusIdentityService.Identity identity) {
        if (!"TEACHER".equals(identity.role())) return "Only a teacher can confirm an extra class booking.";
        String key = identity.participantCode().toLowerCase(Locale.ROOT);
        var pending = pendingBookings.remove(key);
        if (pending == null) return "There is no pending booking. Ask me to schedule an extra class first.";
        var booking = bookingService.confirm(pending);
        messageService.sendSectionSystemNotice(booking.sectionCode(), "Extra class scheduled: " +
                booking.subjectCode() + " in " + booking.roomNumber() + " on " + booking.classDate() +
                " from " + booking.startTime() + " to " + booking.endTime() + ".");
        return "Extra class confirmed in " + booking.roomNumber() + " on " + booking.classDate() + " from " +
                booking.startTime() + " to " + booking.endTime() + ". The section has been notified.";
    }

    public record Result(CampusMessageResponse userMessage, String reply) { }
}
