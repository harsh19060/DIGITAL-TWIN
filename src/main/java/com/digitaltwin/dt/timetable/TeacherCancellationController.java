package com.digitaltwin.dt.timetable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/timetable/cancellations")
public class TeacherCancellationController {
    private final TeacherCancellationService cancellationService;

    public TeacherCancellationController(TeacherCancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping("/teacher/{teacherCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherCancellationService.CancellationResponse cancelTeacherClasses(
            @PathVariable String teacherCode,
            @Valid @RequestBody CancellationRequest request) {
        return cancellationService.cancelClasses(teacherCode,
                new TeacherCancellationService.CancellationRequest(request.classDate(), request.reason()));
    }

    public record CancellationRequest(@NotNull LocalDate classDate,
                                      @NotBlank @Size(max = 300) String reason) { }
}
