package com.digitaltwin.dt.bookings;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "room_bookings", uniqueConstraints = @UniqueConstraint(
        name = "uk_booking_resource_time", columnNames = {"room_id", "class_date", "start_time", "end_time"}))
public class RoomBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false)
    private StudentSection section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookingStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected RoomBooking() { }

    public RoomBooking(Room room, Teacher teacher, StudentSection section, Subject subject,
                       LocalDate classDate, LocalTime startTime, LocalTime endTime) {
        this.room = room;
        this.teacher = teacher;
        this.section = section;
        this.subject = subject;
        this.classDate = classDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Teacher getTeacher() { return teacher; }
    public StudentSection getSection() { return section; }
    public Subject getSubject() { return subject; }
    public LocalDate getClassDate() { return classDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }

    public void cancel(String reason) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }
}

