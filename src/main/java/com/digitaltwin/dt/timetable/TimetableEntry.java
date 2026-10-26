package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.rooms.Room;
import com.digitaltwin.dt.sections.StudentSection;
import com.digitaltwin.dt.subjects.Subject;
import com.digitaltwin.dt.teachers.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private StudentSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_pattern", nullable = false, length = 12)
    private WeekPattern weekPattern;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected TimetableEntry() { }

    public TimetableEntry(Room room, Teacher teacher, StudentSection section, Subject subject,
                          DayOfWeek dayOfWeek, WeekPattern weekPattern,
                          LocalTime startTime, LocalTime endTime) {
        this.room = room;
        this.teacher = teacher;
        this.section = section;
        this.subject = subject;
        this.dayOfWeek = dayOfWeek;
        this.weekPattern = weekPattern;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Teacher getTeacher() { return teacher; }
    public StudentSection getSection() { return section; }
    public Subject getSubject() { return subject; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public WeekPattern getWeekPattern() { return weekPattern; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
