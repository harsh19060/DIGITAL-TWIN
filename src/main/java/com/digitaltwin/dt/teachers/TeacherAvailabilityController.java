package com.digitaltwin.dt.teachers;

import com.digitaltwin.dt.timetable.TimetableEntry;
import com.digitaltwin.dt.timetable.TimetableEntryRepository;
import com.digitaltwin.dt.timetable.WeekPattern;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/teachers/{staffCode}/availability")
public class TeacherAvailabilityController {
    private static final LocalTime OPEN_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(19, 0);

    private final TeacherRepository teacherRepository;
    private final TimetableEntryRepository timetableRepository;

    public TeacherAvailabilityController(TeacherRepository teacherRepository,
                                         TimetableEntryRepository timetableRepository) {
        this.teacherRepository = teacherRepository;
        this.timetableRepository = timetableRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(
            @PathVariable String staffCode,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam WeekPattern weekPattern,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)
                || startTime.isBefore(OPEN_TIME) || endTime.isAfter(CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requested time must be within 07:00–19:00 and end after start");
        }

        Teacher teacher = teacherRepository.findByStaffCodeIgnoreCase(staffCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Teacher not found: " + staffCode));

        Optional<TimetableEntry> conflict = timetableRepository.findAllByDayOfWeek(dayOfWeek).stream()
                .filter(entry -> entry.getTeacher().getId().equals(teacher.getId()))
                .filter(entry -> weeksOverlap(entry.getWeekPattern(), weekPattern))
                .filter(entry -> entry.getStartTime().isBefore(endTime)
                        && startTime.isBefore(entry.getEndTime()))
                .findFirst();

        return conflict
                .map(entry -> new AvailabilityResponse(teacher.getStaffCode(), teacher.getFullName(), false,
                        "SCHEDULED_CLASS", "A class is scheduled during this time", ConflictClass.from(entry)))
                .orElseGet(() -> new AvailabilityResponse(teacher.getStaffCode(), teacher.getFullName(), true,
                        "NO_SCHEDULED_CLASS", "No class is scheduled during this time", null));
    }

    private boolean weeksOverlap(WeekPattern scheduled, WeekPattern requested) {
        return scheduled == WeekPattern.EVERY_WEEK
                || requested == WeekPattern.EVERY_WEEK
                || scheduled == requested;
    }

    public record AvailabilityResponse(String teacherCode, String teacherName, boolean available,
                                       String status, String message, ConflictClass scheduledClass) { }

    public record ConflictClass(String subjectCode, String subjectName, String sectionCode,
                                String roomNumber, LocalTime startTime, LocalTime endTime) {
        static ConflictClass from(TimetableEntry entry) {
            return new ConflictClass(entry.getSubject().getCode(), entry.getSubject().getName(),
                    entry.getSection().getCode(), entry.getRoom().getRoomNumber(),
                    entry.getStartTime(), entry.getEndTime());
        }
    }
}
