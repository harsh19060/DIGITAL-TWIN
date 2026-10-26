package com.digitaltwin.dt.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "message_read_states", uniqueConstraints = @UniqueConstraint(
        name = "uk_message_read_state_participant_conversation",
        columnNames = {"participant_type", "participant_code", "conversation_key"}))
public class MessageReadState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 12)
    private ParticipantType participantType;

    @Column(name = "participant_code", nullable = false, length = 60)
    private String participantCode;

    @Column(name = "conversation_key", nullable = false, length = 180)
    private String conversationKey;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    protected MessageReadState() { }

    public MessageReadState(ParticipantType participantType, String participantCode,
                            String conversationKey, Instant lastReadAt) {
        this.participantType = participantType;
        this.participantCode = participantCode;
        this.conversationKey = conversationKey;
        this.lastReadAt = lastReadAt;
    }

    public Long getId() { return id; }
    public ParticipantType getParticipantType() { return participantType; }
    public String getParticipantCode() { return participantCode; }
    public String getConversationKey() { return conversationKey; }
    public Instant getLastReadAt() { return lastReadAt; }
    public void markReadAt(Instant value) { this.lastReadAt = value; }
}