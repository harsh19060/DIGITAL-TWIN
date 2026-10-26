package com.digitaltwin.dt.sections;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/sections")
public class SectionController {
    private final SectionRepository repository;
    public SectionController(SectionRepository repository) { this.repository = repository; }

    @GetMapping
    public List<StudentSection> list() { return repository.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentSection create(@Valid @RequestBody CreateSectionRequest request) {
        String code = request.code().trim();
        if (repository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section code already exists");
        }
        return repository.save(new StudentSection(code, request.studentCount()));
    }

    public record CreateSectionRequest(@NotBlank @Size(max = 40) String code,
                                       @NotNull @Min(1) Integer studentCount) { }
}
