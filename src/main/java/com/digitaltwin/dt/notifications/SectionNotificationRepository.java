package com.digitaltwin.dt.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionNotificationRepository extends JpaRepository<SectionNotification, Long> {
    List<SectionNotification> findAllBySection_CodeIgnoreCaseOrderByCreatedAtDesc(String sectionCode);
}
