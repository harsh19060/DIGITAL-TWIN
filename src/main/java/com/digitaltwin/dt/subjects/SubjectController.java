package com.digitaltwin.dt.subjects;

import jakarta.validation.Valid;
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
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectRepository repository;
    public SubjectController(SubjectRepository repository) { this.repository = repository; }

    @GetMapping
    public List<Subject> list() { return repository.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject create(@Valid @RequestBody CreateSubjectRequest request) {
        String code = request.code().trim();
        if (repository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists");
        }
        return repository.save(new Subject(code, request.name().trim()));
    }

    public record CreateSubjectRequest(@NotBlank @Size(max = 40) String code,
                                       @NotBlank @Size(max = 120) String name) { }
}
