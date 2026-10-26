package com.digitaltwin.dt.timetable;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/timetable/on-date")
public class DatedTimetableController {
    private final DatedTimetableService datedTimetableService;
    public DatedTimetableController(DatedTimetableService datedTimetableService) {
        this.datedTimetableService = datedTimetableService;
    }

    @GetMapping
    public DatedTimetableService.DatedScheduleResponse onDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return datedTimetableService.onDate(date);
    }
}
