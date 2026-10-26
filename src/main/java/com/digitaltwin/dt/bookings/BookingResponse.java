package com.digitaltwin.dt.bookings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(Long id, String roomNumber, String teacherCode, String teacherName,
                              String sectionCode, String subjectCode, LocalDate classDate,
                              LocalTime startTime, LocalTime endTime, BookingStatus status,
                              Instant createdAt, Instant cancelledAt, String cancellationReason) {
    static BookingResponse from(RoomBooking booking) {
        return new BookingResponse(booking.getId(), booking.getRoom().getRoomNumber(),
                booking.getTeacher().getStaffCode(), booking.getTeacher().getFullName(),
                booking.getSection().getCode(), booking.getSubject().getCode(), booking.getClassDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getStatus(), booking.getCreatedAt(),
                booking.getCancelledAt(), booking.getCancellationReason());
    }
}
