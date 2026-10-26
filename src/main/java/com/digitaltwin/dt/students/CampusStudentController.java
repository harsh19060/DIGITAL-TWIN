package com.digitaltwin.dt.students;

import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.sections.StudentSection;
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
@RequestMapping("/api/students")
public class CampusStudentController {
    private final CampusStudentRepository studentRepository;
    private final SectionRepository sectionRepository;

    public CampusStudentController(CampusStudentRepository studentRepository, SectionRepository sectionRepository) {
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
    }

    @GetMapping
    public List<CampusStudentResponse> list() {
        return studentRepository.findAll().stream().map(CampusStudentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusStudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        String code = request.studentCode().trim();
        if (studentRepository.findByStudentCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student code already exists");
        }
        StudentSection section = sectionRepository.findByCodeIgnoreCase(request.sectionCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Section not found: " + request.sectionCode()));
        return CampusStudentResponse.from(studentRepository.save(new CampusStudent(code,
                request.fullName().trim(), section)));
    }

    public record CreateStudentRequest(@NotBlank @Size(max = 60) String studentCode,
                                       @NotBlank @Size(max = 120) String fullName,
                                       @NotBlank @Size(max = 40) String sectionCode) { }
}
