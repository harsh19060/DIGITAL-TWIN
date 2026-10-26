package com.digitaltwin.dt.messages;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CampusMessageRepository extends JpaRepository<CampusMessage, Long> {
    List<CampusMessage> findAllByAudienceOrderByCreatedAtDesc(MessageAudience audience, Pageable pageable);

    List<CampusMessage> findAllByAudienceAndRecipientCodeIgnoreCaseOrderByCreatedAtDesc(
            MessageAudience audience, String recipientCode, Pageable pageable);

    @Query("select m from CampusMessage m where m.audience = :audience and " +
            "((m.senderType = :firstType and lower(m.senderCode) = lower(:firstCode) " +
            "and m.recipientType = :secondType and lower(m.recipientCode) = lower(:secondCode)) or " +
            "(m.senderType = :secondType and lower(m.senderCode) = lower(:secondCode) " +
            "and m.recipientType = :firstType and lower(m.recipientCode) = lower(:firstCode))) " +
            "order by m.createdAt desc")
    List<CampusMessage> findDirectConversation(@Param("audience") MessageAudience audience,
                                               @Param("firstType") ParticipantType firstType,
                                               @Param("firstCode") String firstCode,
                                               @Param("secondType") ParticipantType secondType,
                                               @Param("secondCode") String secondCode,
                                               Pageable pageable);
}
