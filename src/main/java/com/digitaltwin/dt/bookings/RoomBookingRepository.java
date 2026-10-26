package com.digitaltwin.dt.bookings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {
    List<RoomBooking> findAllByClassDateAndStatusOrderByStartTimeAsc(LocalDate classDate, BookingStatus status);
    List<RoomBooking> findAllByClassDateOrderByStartTimeAsc(LocalDate classDate);
}

