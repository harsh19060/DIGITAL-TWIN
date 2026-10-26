package com.digitaltwin.dt.timetable;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_changes", uniqueConstraints = @UniqueConstraint(
        name = "uk_schedule_change_entry_date", columnNames = {"timetable_entry_id", "class_date"}))
public class ScheduleChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private TimetableEntry timetableEntry;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ScheduleChangeType changeType;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "created_by", nullable = false, length = 40)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScheduleChange() { }

    public ScheduleChange(TimetableEntry timetableEntry, LocalDate classDate,
                          ScheduleChangeType changeType, String reason, String createdBy) {
        this.timetableEntry = timetableEntry;
        this.classDate = classDate;
        this.changeType = changeType;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public TimetableEntry getTimetableEntry() { return timetableEntry; }
    public LocalDate getClassDate() { return classDate; }
    public ScheduleChangeType getChangeType() { return changeType; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
