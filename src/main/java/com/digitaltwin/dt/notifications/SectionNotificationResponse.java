package com.digitaltwin.dt.notifications;

import java.time.Instant;
import java.time.LocalDate;

public record SectionNotificationResponse(Long id, String sectionCode, LocalDate classDate,
                                          String title, String message, Instant createdAt) {
    static SectionNotificationResponse from(SectionNotification notification) {
        return new SectionNotificationResponse(notification.getId(), notification.getSection().getCode(),
                notification.getClassDate(), notification.getTitle(), notification.getMessage(),
                notification.getCreatedAt());
    }
}
