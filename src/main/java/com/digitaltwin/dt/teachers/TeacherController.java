package com.digitaltwin.dt.teachers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    private final TeacherRepository repository;
    public TeacherController(TeacherRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Teacher> list() { return repository.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Teacher create(@Valid @RequestBody CreateTeacherRequest request) {
        String code = request.staffCode().trim();
        if (repository.findByStaffCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher code already exists");
        }
        return repository.save(new Teacher(code, request.fullName().trim(), blankToNull(request.email())));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateTeacherRequest(
            @NotBlank @Size(max = 40) String staffCode,
            @NotBlank @Size(max = 120) String fullName,
            @Email @Size(max = 160) String email) { }
}
