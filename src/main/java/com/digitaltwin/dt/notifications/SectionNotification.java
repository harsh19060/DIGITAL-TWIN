package com.digitaltwin.dt.notifications;

import com.digitaltwin.dt.sections.StudentSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "section_notifications")
public class SectionNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private StudentSection section;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SectionNotification() { }

    public SectionNotification(StudentSection section, LocalDate classDate, String title, String message) {
        this.section = section;
        this.classDate = classDate;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public StudentSection getSection() { return section; }
    public LocalDate getClassDate() { return classDate; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
