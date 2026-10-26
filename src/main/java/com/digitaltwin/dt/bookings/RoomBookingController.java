package com.digitaltwin.dt.bookings;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class RoomBookingController {
    private final RoomBookingService bookingService;
    public RoomBookingController(RoomBookingService bookingService) { this.bookingService = bookingService; }

    @PostMapping("/preview")
    public RoomBookingService.BookingPreview preview(
            @Valid @RequestBody RoomBookingService.BookingRequest request) {
        return bookingService.preview(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse confirm(@Valid @RequestBody RoomBookingService.BookingRequest request) {
        return bookingService.confirm(request);
    }

    @GetMapping
    public List<BookingResponse> listForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listForDate(date);
    }
}
