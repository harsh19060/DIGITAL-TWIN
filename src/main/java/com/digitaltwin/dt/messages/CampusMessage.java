package com.digitaltwin.dt.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "campus_messages")
public class CampusMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 12)
    private ParticipantType senderType;

    @Column(name = "sender_code", nullable = false, length = 60)
    private String senderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MessageAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", length = 12)
    private ParticipantType recipientType;

    @Column(name = "recipient_code", length = 60)
    private String recipientCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageCategory category;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CampusMessage() { }

    public CampusMessage(ParticipantType senderType, String senderCode, MessageAudience audience,
                         ParticipantType recipientType, String recipientCode,
                         MessageCategory category, String body) {
        this.senderType = senderType;
        this.senderCode = senderCode;
        this.audience = audience;
        this.recipientType = recipientType;
        this.recipientCode = recipientCode;
        this.category = category;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public ParticipantType getSenderType() { return senderType; }
    public String getSenderCode() { return senderCode; }
    public MessageAudience getAudience() { return audience; }
    public ParticipantType getRecipientType() { return recipientType; }
    public String getRecipientCode() { return recipientCode; }
    public MessageCategory getCategory() { return category; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
