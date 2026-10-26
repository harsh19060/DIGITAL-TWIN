package com.digitaltwin.dt.calendar;

import com.digitaltwin.dt.timetable.WeekPattern;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekCycleServiceTest {
    private final WeekCycleService service = new WeekCycleService();
    private final LocalDate weekAStart = LocalDate.of(2026, 10, 26);

    @Test
    void marksAnchorWeekAsWeekAAndAlternatesByWeek() {
        assertEquals(WeekPattern.WEEK_A, service.patternFor(weekAStart, weekAStart));
        assertEquals(WeekPattern.WEEK_A, service.patternFor(weekAStart.plusDays(5), weekAStart));
        assertEquals(WeekPattern.WEEK_B, service.patternFor(weekAStart.plusWeeks(1), weekAStart));
        assertEquals(WeekPattern.WEEK_A, service.patternFor(weekAStart.plusWeeks(2), weekAStart));
        assertEquals(WeekPattern.WEEK_B, service.patternFor(weekAStart.minusWeeks(1), weekAStart));
    }
}
