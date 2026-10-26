package com.digitaltwin.dt.notifications;

import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.sections.StudentSection;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/sections/{sectionCode}/notifications")
public class SectionNotificationController {
    private final SectionRepository sectionRepository;
    private final SectionNotificationRepository notificationRepository;

    public SectionNotificationController(SectionRepository sectionRepository,
                                         SectionNotificationRepository notificationRepository) {
        this.sectionRepository = sectionRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SectionNotificationResponse> listForSection(@PathVariable String sectionCode) {
        StudentSection section = sectionRepository.findByCodeIgnoreCase(sectionCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Section not found: " + sectionCode));
        return notificationRepository.findAllBySection_CodeIgnoreCaseOrderByCreatedAtDesc(section.getCode())
                .stream().map(SectionNotificationResponse::from).toList();
    }
}
