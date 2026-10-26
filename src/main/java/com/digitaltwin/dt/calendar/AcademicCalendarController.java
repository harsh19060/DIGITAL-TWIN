package com.digitaltwin.dt.calendar;

import com.digitaltwin.dt.timetable.WeekPattern;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/academic-calendar")
public class AcademicCalendarController {
    private final AcademicCalendarRepository repository;
    private final WeekCycleService weekCycleService = new WeekCycleService();

    public AcademicCalendarController(AcademicCalendarRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CalendarResponse getSettings() {
        return repository.findById(1L)
                .map(settings -> new CalendarResponse(true, settings.getWeekAStartDate()))
                .orElseGet(() -> new CalendarResponse(false, null));
    }

    @PutMapping
    @Transactional
    public CalendarResponse setWeekAStart(@Valid @RequestBody SetCalendarRequest request) {
        if (request.weekAStartDate().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Week A start date must be a Monday");
        }
        AcademicCalendarSettings settings = repository.findById(1L)
                .map(existing -> {
                    existing.updateWeekAStartDate(request.weekAStartDate());
                    return existing;
                })
                .orElseGet(() -> new AcademicCalendarSettings(request.weekAStartDate()));
        repository.save(settings);
        return new CalendarResponse(true, settings.getWeekAStartDate());
    }

    @GetMapping("/week-pattern")
    @Transactional(readOnly = true)
    public WeekPatternResponse patternForDate(@RequestParam LocalDate date) {
        AcademicCalendarSettings settings = repository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Admin must set the Week A start date first"));
        WeekPattern pattern = weekCycleService.patternFor(date, settings.getWeekAStartDate());
        return new WeekPatternResponse(date, pattern);
    }

    public record SetCalendarRequest(@NotNull LocalDate weekAStartDate) { }
    public record CalendarResponse(boolean configured, LocalDate weekAStartDate) { }
    public record WeekPatternResponse(LocalDate date, WeekPattern weekPattern) { }
}
