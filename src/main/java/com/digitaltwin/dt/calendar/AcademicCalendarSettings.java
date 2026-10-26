package com.digitaltwin.dt.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "academic_calendar")
public class AcademicCalendarSettings {
    @Id
    private Long id;

    @Column(name = "week_a_start_date", nullable = false)
    private LocalDate weekAStartDate;

    protected AcademicCalendarSettings() { }

    public AcademicCalendarSettings(LocalDate weekAStartDate) {
        this.id = 1L;
        this.weekAStartDate = weekAStartDate;
    }

    public Long getId() { return id; }
    public LocalDate getWeekAStartDate() { return weekAStartDate; }

    public void updateWeekAStartDate(LocalDate weekAStartDate) {
        this.weekAStartDate = weekAStartDate;
    }
}
