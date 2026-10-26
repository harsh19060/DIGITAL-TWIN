package com.digitaltwin.dt.calendar;

import com.digitaltwin.dt.timetable.WeekPattern;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class WeekCycleService {
    public WeekPattern patternFor(LocalDate date, LocalDate weekAStartDate) {
        if (weekAStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Week A start date must be a Monday");
        }
        LocalDate dateWeekMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weeksFromAnchor = ChronoUnit.WEEKS.between(weekAStartDate, dateWeekMonday);
        return Math.floorMod(weeksFromAnchor, 2) == 0 ? WeekPattern.WEEK_A : WeekPattern.WEEK_B;
    }
}
