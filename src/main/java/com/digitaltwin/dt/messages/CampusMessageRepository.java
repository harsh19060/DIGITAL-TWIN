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

    @Query("select m from CampusMessage m where m.audience = :audience and " +
            "((m.senderType = :type and lower(m.senderCode) = lower(:code)) or " +
            "(m.recipientType = :type and lower(m.recipientCode) = lower(:code))) order by m.createdAt desc")
    List<CampusMessage> findDirectForParticipant(@Param("audience") MessageAudience audience,
                                                 @Param("type") ParticipantType type,
                                                 @Param("code") String code, Pageable pageable);

    @Query("select m from CampusMessage m where m.audience = :audience and " +
            "(lower(m.recipientCode) = lower(:sectionCode) or " +
            "(m.senderType = :senderType and lower(m.senderCode) = lower(:senderCode))) order by m.createdAt desc")
    List<CampusMessage> findSectionConversationForParticipant(@Param("audience") MessageAudience audience,
                                                              @Param("sectionCode") String sectionCode,
                                                              @Param("senderType") ParticipantType senderType,
                                                              @Param("senderCode") String senderCode,
                                                              Pageable pageable);

    @Query("select count(m) from CampusMessage m where m.audience = :audience and m.createdAt > :after " +
            "and m.senderType = :peerType and lower(m.senderCode) = lower(:peerCode) " +
            "and m.recipientType = :participantType and lower(m.recipientCode) = lower(:participantCode)")
    long countIncomingDirect(@Param("audience") MessageAudience audience,
                             @Param("peerType") ParticipantType peerType, @Param("peerCode") String peerCode,
                             @Param("participantType") ParticipantType participantType,
                             @Param("participantCode") String participantCode,
                             @Param("after") java.time.Instant after);

    @Query("select count(m) from CampusMessage m where m.audience = :audience and " +
            "lower(m.recipientCode) = lower(:sectionCode) and m.createdAt > :after and " +
            "(m.senderType <> :participantType or lower(m.senderCode) <> lower(:participantCode))")
    long countIncomingSection(@Param("audience") MessageAudience audience, @Param("sectionCode") String sectionCode,
                              @Param("participantType") ParticipantType participantType,
                              @Param("participantCode") String participantCode,
                              @Param("after") java.time.Instant after);

    @Query("select count(m) from CampusMessage m where m.audience = :audience and m.createdAt > :after and " +
            "(m.senderType <> :participantType or lower(m.senderCode) <> lower(:participantCode))")
    long countIncomingGlobal(@Param("audience") MessageAudience audience,
                             @Param("participantType") ParticipantType participantType,
                             @Param("participantCode") String participantCode,
                             @Param("after") java.time.Instant after);}
