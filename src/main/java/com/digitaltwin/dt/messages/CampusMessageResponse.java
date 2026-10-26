package com.digitaltwin.dt.messages;

import java.time.Instant;

public record CampusMessageResponse(Long id, ParticipantType senderType, String senderCode,
                                    MessageAudience audience, ParticipantType recipientType,
                                    String recipientCode, MessageCategory category,
                                    String body, Instant createdAt) {
    static CampusMessageResponse from(CampusMessage message) {
        return new CampusMessageResponse(message.getId(), message.getSenderType(), message.getSenderCode(),
                message.getAudience(), message.getRecipientType(), message.getRecipientCode(),
                message.getCategory(), message.getBody(), message.getCreatedAt());
    }
}
