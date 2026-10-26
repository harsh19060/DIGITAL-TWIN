package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.calendar.AcademicCalendarRepository;
import com.digitaltwin.dt.calendar.AcademicCalendarSettings;
import com.digitaltwin.dt.notifications.SectionNotificationRepository;
import com.digitaltwin.dt.rooms.Room;
import com.digitaltwin.dt.rooms.RoomRepository;
import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.sections.StudentSection;
import com.digitaltwin.dt.subjects.Subject;
import com.digitaltwin.dt.subjects.SubjectRepository;
import com.digitaltwin.dt.teachers.Teacher;
import com.digitaltwin.dt.teachers.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(TeacherCancellationService.class)
class TeacherCancellationServiceTest {
    @Autowired RoomRepository roomRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired SectionRepository sectionRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired TimetableEntryRepository timetableRepository;
    @Autowired ScheduleChangeRepository changeRepository;
    @Autowired SectionNotificationRepository notificationRepository;
    @Autowired AcademicCalendarRepository calendarRepository;
    @Autowired TeacherCancellationService cancellationService;

    private final LocalDate weekAStart = LocalDate.of(2026, 10, 26);

    @Test
    void cancelsOnlyTheActiveWeekAndNotifiesTheSectionOnce() {
        Fixture fixture = createFixture();
        calendarRepository.save(new AcademicCalendarSettings(weekAStart));
        timetableRepository.save(new TimetableEntry(fixture.room, fixture.teacher, fixture.section,
                fixture.subject, DayOfWeek.MONDAY, WeekPattern.WEEK_A,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        timetableRepository.save(new TimetableEntry(fixture.room, fixture.teacher, fixture.section,
                fixture.subject, DayOfWeek.MONDAY, WeekPattern.WEEK_B,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));

        var first = cancellationService.cancelClasses("T001",
                new TeacherCancellationService.CancellationRequest(weekAStart, "Sick leave"));
        var repeated = cancellationService.cancelClasses("T001",
                new TeacherCancellationService.CancellationRequest(weekAStart, "Sick leave"));

        assertEquals(WeekPattern.WEEK_A, first.activeWeek());
        assertEquals(1, first.cancelledClassCount());
        assertEquals(1, changeRepository.count());
        assertEquals(1, notificationRepository.count());
        assertEquals(2, timetableRepository.count(), "Base timetable entries must remain unchanged");
        assertEquals(0, repeated.cancelledClassCount(), "Repeating a cancellation must be idempotent");
    }

    @Test
    void requiresWeekAAnchorWhenTeacherHasAlternatingClasses() {
        Fixture fixture = createFixture();
        timetableRepository.save(new TimetableEntry(fixture.room, fixture.teacher, fixture.section,
                fixture.subject, DayOfWeek.MONDAY, WeekPattern.WEEK_A,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> cancellationService.cancelClasses("T001",
                        new TeacherCancellationService.CancellationRequest(weekAStart, "Sick leave")));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(0, changeRepository.count());
        assertEquals(0, notificationRepository.count());
    }

    private Fixture createFixture() {
        Room room = roomRepository.save(new Room("DT901", 9, 40, "classroom", "whiteboard"));
        Teacher teacher = teacherRepository.save(new Teacher("T001", "Dr. Sharma", "sharma@example.edu"));
        StudentSection section = sectionRepository.save(new StudentSection("A7", 35));
        Subject subject = subjectRepository.save(new Subject("DBMS", "Database Management Systems"));
        return new Fixture(room, teacher, section, subject);
    }

    private record Fixture(Room room, Teacher teacher, StudentSection section, Subject subject) { }
}

