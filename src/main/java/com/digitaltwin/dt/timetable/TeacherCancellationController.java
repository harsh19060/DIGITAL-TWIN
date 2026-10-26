package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.messages.ParticipantType;
import com.digitaltwin.dt.session.CampusIdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/timetable/cancellations")
public class TeacherCancellationController {
    private final TeacherCancellationService cancellationService;
    private final CampusIdentityService identityService;

    public TeacherCancellationController(TeacherCancellationService cancellationService,
                                         CampusIdentityService identityService) {
        this.cancellationService = cancellationService;
        this.identityService = identityService;
    }

    @PostMapping("/teacher/{teacherCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherCancellationService.CancellationResponse cancelTeacherClasses(
            @PathVariable String teacherCode,
            @Valid @RequestBody CancellationRequest request,
            Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        if (identity.participantType() != ParticipantType.TEACHER
                || !identity.participantCode().equalsIgnoreCase(teacherCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers can only cancel their own classes");
        }
        return cancellationService.cancelClasses(teacherCode,
                new TeacherCancellationService.CancellationRequest(request.classDate(), request.reason()));
    }

    public record CancellationRequest(@NotNull LocalDate classDate,
                                      @NotBlank @Size(max = 300) String reason) { }
}
