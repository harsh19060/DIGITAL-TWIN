package com.digitaltwin.dt.bookings;

import com.digitaltwin.dt.session.CampusIdentityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class RoomBookingController {
    private final RoomBookingService bookingService;
    private final CampusIdentityService identityService;

    public RoomBookingController(RoomBookingService bookingService, CampusIdentityService identityService) {
        this.bookingService = bookingService;
        this.identityService = identityService;
    }

    @PostMapping("/preview")
    public RoomBookingService.BookingPreview preview(
            @Valid @RequestBody RoomBookingService.BookingRequest request, Authentication authentication) {
        assertTeacher(request.teacherCode(), authentication);
        return bookingService.preview(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse confirm(@Valid @RequestBody RoomBookingService.BookingRequest request,
                                   Authentication authentication) {
        assertTeacher(request.teacherCode(), authentication);
        return bookingService.confirm(request);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable Long bookingId,
                                  @Valid @RequestBody CancellationRequest request,
                                  Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        if (identity.participantType() != com.digitaltwin.dt.messages.ParticipantType.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can cancel extra classes");
        }
        return bookingService.cancel(bookingId, request.reason(), identity.participantCode());
    }

    @GetMapping
    public List<BookingResponse> listForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listForDate(date);
    }

    private void assertTeacher(String teacherCode, Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        if (identity.participantType() != com.digitaltwin.dt.messages.ParticipantType.TEACHER
                || !identity.participantCode().equalsIgnoreCase(teacherCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers can only book classes for themselves");
        }
    }

    public record CancellationRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 300) String reason) { }
}
