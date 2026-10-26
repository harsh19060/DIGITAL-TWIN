package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.calendar.AcademicCalendarRepository;
import com.digitaltwin.dt.calendar.AcademicCalendarSettings;
import com.digitaltwin.dt.calendar.WeekCycleService;
import com.digitaltwin.dt.notifications.SectionNotification;
import com.digitaltwin.dt.notifications.SectionNotificationRepository;
import com.digitaltwin.dt.sections.StudentSection;
import com.digitaltwin.dt.teachers.Teacher;
import com.digitaltwin.dt.teachers.TeacherRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service
public class TeacherCancellationService {
    private final TeacherRepository teacherRepository;
    private final TimetableEntryRepository timetableRepository;
    private final ScheduleChangeRepository changeRepository;
    private final SectionNotificationRepository notificationRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final WeekCycleService weekCycleService = new WeekCycleService();

    public TeacherCancellationService(TeacherRepository teacherRepository,
                                      TimetableEntryRepository timetableRepository,
                                      ScheduleChangeRepository changeRepository,
                                      SectionNotificationRepository notificationRepository,
                                      AcademicCalendarRepository calendarRepository) {
        this.teacherRepository = teacherRepository;
        this.timetableRepository = timetableRepository;
        this.changeRepository = changeRepository;
        this.notificationRepository = notificationRepository;
        this.calendarRepository = calendarRepository;
    }

    @Transactional
    public CancellationResponse cancelClasses(String teacherCode, @Valid CancellationRequest request) {
        Teacher teacher = teacherRepository.findByStaffCodeIgnoreCase(teacherCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Teacher not found: " + teacherCode));
        DayOfWeek dayOfWeek = request.classDate().getDayOfWeek();

        List<TimetableEntry> teacherEntries = timetableRepository.findAllByDayOfWeek(dayOfWeek).stream()
                .filter(entry -> entry.getTeacher().getId().equals(teacher.getId()))
                .toList();

        boolean hasAlternatingClasses = teacherEntries.stream()
                .anyMatch(entry -> entry.getWeekPattern() != WeekPattern.EVERY_WEEK);
        WeekPattern activeWeek = WeekPattern.EVERY_WEEK;
        if (hasAlternatingClasses) {
            AcademicCalendarSettings settings = calendarRepository.findById(1L)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Admin must set the Week A start date before cancelling an alternating-week class"));
            activeWeek = weekCycleService.patternFor(request.classDate(), settings.getWeekAStartDate());
        }

        List<CanceledClass> cancelled = new ArrayList<>();
        List<SectionNotification> notifications = new ArrayList<>();
        String reason = request.reason().trim();
        for (TimetableEntry entry : teacherEntries) {
            if (entry.getWeekPattern() != WeekPattern.EVERY_WEEK && entry.getWeekPattern() != activeWeek) {
                continue;
            }
            if (changeRepository.existsByTimetableEntry_IdAndClassDate(entry.getId(), request.classDate())) {
                continue;
            }

            changeRepository.save(new ScheduleChange(entry, request.classDate(),
                    ScheduleChangeType.CANCELLED, reason, teacher.getStaffCode()));
            StudentSection section = entry.getSection();
            String title = "Class cancelled: " + entry.getSubject().getCode();
            String message = String.format("%s reports: %s in %s from %s to %s is cancelled on %s. Reason: %s.",
                    teacher.getFullName(), entry.getSubject().getName(), entry.getRoom().getRoomNumber(),
                    entry.getStartTime(), entry.getEndTime(), request.classDate(), reason);
            notifications.add(new SectionNotification(section, request.classDate(), title, message));
            cancelled.add(CanceledClass.from(entry));
        }
        notificationRepository.saveAll(notifications);

        List<String> notifiedSections = new TreeSet<>(cancelled.stream()
                .map(CanceledClass::sectionCode).toList()).stream().toList();
        return new CancellationResponse(teacher.getStaffCode(), teacher.getFullName(), request.classDate(),
                activeWeek, cancelled.size(), cancelled, notifiedSections);
    }

    public record CancellationRequest(@NotNull LocalDate classDate,
                                      @NotBlank @Size(max = 300) String reason) { }

    public record CancellationResponse(String teacherCode, String teacherName, LocalDate classDate,
                                       WeekPattern activeWeek, int cancelledClassCount,
                                       List<CanceledClass> cancelledClasses, List<String> notifiedSections) { }

    public record CanceledClass(Long timetableEntryId, String subjectCode, String sectionCode,
                                String roomNumber, LocalTime startTime, LocalTime endTime) {
        static CanceledClass from(TimetableEntry entry) {
            return new CanceledClass(entry.getId(), entry.getSubject().getCode(), entry.getSection().getCode(),
                    entry.getRoom().getRoomNumber(), entry.getStartTime(), entry.getEndTime());
        }
    }
}
