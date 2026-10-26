package com.digitaltwin.dt.messages;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MessageReadStateRepository extends JpaRepository<MessageReadState, Long> {
    Optional<MessageReadState> findByParticipantTypeAndParticipantCodeIgnoreCaseAndConversationKey(
            ParticipantType participantType, String participantCode, String conversationKey);
}