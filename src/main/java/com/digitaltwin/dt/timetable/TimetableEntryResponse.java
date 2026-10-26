package com.digitaltwin.dt.timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableEntryResponse(Long id, String roomNumber, String teacherCode, String teacherName,
                                     String sectionCode, String subjectCode, String subjectName,
                                     DayOfWeek dayOfWeek, WeekPattern weekPattern,
                                     LocalTime startTime, LocalTime endTime) {
    static TimetableEntryResponse from(TimetableEntry entry) {
        return new TimetableEntryResponse(entry.getId(), entry.getRoom().getRoomNumber(),
                entry.getTeacher().getStaffCode(), entry.getTeacher().getFullName(),
                entry.getSection().getCode(), entry.getSubject().getCode(), entry.getSubject().getName(),
                entry.getDayOfWeek(), entry.getWeekPattern(), entry.getStartTime(), entry.getEndTime());
    }
}
