package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.calendar.AcademicCalendarRepository;
import com.digitaltwin.dt.calendar.AcademicCalendarSettings;
import com.digitaltwin.dt.calendar.WeekCycleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DatedTimetableService {
    private final TimetableEntryRepository timetableRepository;
    private final ScheduleChangeRepository changeRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final WeekCycleService weekCycleService = new WeekCycleService();

    public DatedTimetableService(TimetableEntryRepository timetableRepository,
                                 ScheduleChangeRepository changeRepository,
                                 AcademicCalendarRepository calendarRepository) {
        this.timetableRepository = timetableRepository;
        this.changeRepository = changeRepository;
        this.calendarRepository = calendarRepository;
    }

    @Transactional(readOnly = true)
    public DatedScheduleResponse onDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        var entries = timetableRepository.findAllByDayOfWeekOrderByStartTimeAsc(dayOfWeek);
        boolean hasAlternatingEntries = entries.stream()
                .anyMatch(entry -> entry.getWeekPattern() != WeekPattern.EVERY_WEEK);

        WeekPattern activeWeek = WeekPattern.EVERY_WEEK;
        if (hasAlternatingEntries) {
            AcademicCalendarSettings settings = calendarRepository.findById(1L)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Admin must set the Week A start date before viewing an alternating-week schedule"));
            activeWeek = weekCycleService.patternFor(date, settings.getWeekAStartDate());
        }

        WeekPattern finalActiveWeek = activeWeek;
        Map<Long, ScheduleChange> changesByEntryId = changeRepository.findAllByClassDate(date).stream()
                .filter(change -> change.getChangeType() == ScheduleChangeType.CANCELLED)
                .collect(Collectors.toMap(change -> change.getTimetableEntry().getId(), change -> change));

        var classes = entries.stream()
                .filter(entry -> entry.getWeekPattern() == WeekPattern.EVERY_WEEK
                        || entry.getWeekPattern() == finalActiveWeek)
                .map(entry -> DatedClass.from(entry, changesByEntryId.get(entry.getId())))
                .toList();
        return new DatedScheduleResponse(date, activeWeek, classes);
    }

    public record DatedScheduleResponse(LocalDate date, WeekPattern activeWeek,
                                        java.util.List<DatedClass> classes) { }

    public record DatedClass(Long timetableEntryId, String subjectCode, String subjectName,
                             String sectionCode, String teacherName, String roomNumber,
                             LocalTime startTime, LocalTime endTime, String status,
                             String changeReason) {
        static DatedClass from(TimetableEntry entry, ScheduleChange change) {
            return new DatedClass(entry.getId(), entry.getSubject().getCode(), entry.getSubject().getName(),
                    entry.getSection().getCode(), entry.getTeacher().getFullName(),
                    entry.getRoom().getRoomNumber(), entry.getStartTime(), entry.getEndTime(),
                    change == null ? "SCHEDULED" : "CANCELLED",
                    change == null ? null : change.getReason());
        }
    }
}


