package com.digitaltwin.dt.messages;

import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.students.CampusStudentRepository;
import com.digitaltwin.dt.teachers.TeacherRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class CampusMessageService {
    private static final int HISTORY_LIMIT = 100;
    private final CampusMessageRepository messageRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final CampusStudentRepository studentRepository;
    private final CampusMessageStream messageStream;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageReadStateRepository readStateRepository;

    public CampusMessageService(CampusMessageRepository messageRepository, TeacherRepository teacherRepository,
                                SectionRepository sectionRepository, CampusStudentRepository studentRepository,
                                CampusMessageStream messageStream, ApplicationEventPublisher eventPublisher,
                                MessageReadStateRepository readStateRepository) {
        this.messageRepository = messageRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.messageStream = messageStream;
        this.eventPublisher = eventPublisher;
        this.readStateRepository = readStateRepository;
    }

    @Transactional
    public CampusMessageResponse send(SendMessageRequest request) {
        String senderCode = request.senderCode().trim();
        String recipientCode = request.recipientCode() == null ? null : request.recipientCode().trim();
        validateParticipants(request, senderCode, recipientCode);
        return saveMessage(request.senderType(), senderCode, request.audience(), request.recipientType(),
                recipientCode, request.category(), request.body().trim());
    }

    @Transactional
    public CampusMessageResponse sendSystemReply(ParticipantType userType, String userCode,
                                                 String command, String reply) {
        CampusMessageResponse incoming = saveMessage(userType, userCode, MessageAudience.DIRECT,
                ParticipantType.SYSTEM, "SYSTEM", MessageCategory.QUESTION, command);
        saveMessage(ParticipantType.SYSTEM, "SYSTEM", MessageAudience.DIRECT,
                userType, userCode, MessageCategory.GENERAL, reply);
        return incoming;
    }

    @Transactional
    public CampusMessageResponse sendSectionSystemNotice(String sectionCode, String body) {
        String code = sectionCode.trim();
        if (sectionRepository.findByCodeIgnoreCase(code).isEmpty()) throw notFound("Section", code);
        return saveMessage(ParticipantType.SYSTEM, "SYSTEM", MessageAudience.SECTION,
                ParticipantType.SECTION, code, MessageCategory.ANNOUNCEMENT, body);
    }
    private CampusMessageResponse saveMessage(ParticipantType senderType, String senderCode,
                                               MessageAudience audience, ParticipantType recipientType,
                                               String recipientCode, MessageCategory category, String body) {
        CampusMessage saved = messageRepository.save(new CampusMessage(senderType, senderCode, audience,
                recipientType, recipientCode, category, body));
        CampusMessageResponse response = CampusMessageResponse.from(saved);
        eventPublisher.publishEvent(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> contacts(ParticipantType type, String code) {
        Pageable page = PageRequest.of(0, 200);
        java.util.Map<String, ContactResponse> contacts = new java.util.LinkedHashMap<>();
        contacts.put("DIRECT:SYSTEM:system", new ContactResponse("DIRECT", ParticipantType.SYSTEM, "SYSTEM",
                "Campus system", "Ask about free rooms or class changes", null, null, null,
                readAt(type, code, directKey(type, code, ParticipantType.SYSTEM, "SYSTEM")),
                unreadDirect(type, code, ParticipantType.SYSTEM, "SYSTEM")));

        List<CampusMessage> latestGlobal = messageRepository.findAllByAudienceOrderByCreatedAtDesc(
                MessageAudience.GLOBAL, PageRequest.of(0, 1));
        CampusMessage global = latestGlobal.isEmpty() ? null : latestGlobal.get(0);
        contacts.put("GLOBAL", new ContactResponse("GLOBAL", null, "ALL", "Building-wide",
                global == null ? "Messages for everyone in the building" : global.getBody(),
                global == null ? null : global.getCreatedAt(),
                global == null ? null : global.getSenderType(), global == null ? null : global.getSenderCode(),
                readAt(type, code, "global"), unreadGlobal(type, code)));

        for (CampusMessage message : messageRepository.findDirectForParticipant(MessageAudience.DIRECT, type, code, page)) {
            boolean sentByMe = message.getSenderType() == type && message.getSenderCode().equalsIgnoreCase(code);
            ParticipantType otherType = sentByMe ? message.getRecipientType() : message.getSenderType();
            String otherCode = sentByMe ? message.getRecipientCode() : message.getSenderCode();
            String key = "DIRECT:" + otherType + ":" + otherCode.toLowerCase(Locale.ROOT);
            ContactResponse current = contacts.get(key);
            if (current == null || current.lastMessageAt() == null
                    || message.getCreatedAt().isAfter(current.lastMessageAt())) {
                contacts.put(key, new ContactResponse("DIRECT", otherType, otherCode,
                        contactName(otherType, otherCode), message.getBody(), message.getCreatedAt(),
                        message.getSenderType(), message.getSenderCode(),
                        readAt(type, code, directKey(type, code, otherType, otherCode)),
                        unreadDirect(type, code, otherType, otherCode)));
            }
        }

        String sectionCode = null;
        if (type == ParticipantType.STUDENT) {
            sectionCode = studentRepository.findByStudentCodeIgnoreCase(code)
                    .map(student -> student.getSection().getCode()).orElse(null);
        }
        if (sectionCode != null) {
            String sectionKey = "SECTION:" + sectionCode.toLowerCase(Locale.ROOT);
            contacts.put(sectionKey, sectionContact(sectionCode, type, code, page));
        } else if (type == ParticipantType.TEACHER) {
            for (CampusMessage message : messageRepository.findSectionConversationForParticipant(
                    MessageAudience.SECTION, "", type, code, page)) {
                String sentTo = message.getRecipientCode();
                if (sentTo != null && !sentTo.isBlank()) {
                    String key = "SECTION:" + sentTo.toLowerCase(Locale.ROOT);
                    if (!contacts.containsKey(key)) contacts.put(key, sectionContact(sentTo, type, code, page));
                }
            }
        }
        return contacts.values().stream()
                .sorted(java.util.Comparator.comparing(ContactResponse::lastMessageAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    private ContactResponse sectionContact(String sectionCode, ParticipantType type, String code, Pageable page) {
        List<CampusMessage> history = messageRepository.findSectionConversationForParticipant(
                MessageAudience.SECTION, sectionCode, type, code, page);
        CampusMessage latest = history.isEmpty() ? null : history.get(0);
        String key = sectionKey(sectionCode);
        return new ContactResponse("SECTION", ParticipantType.SECTION, sectionCode, "Class " + sectionCode,
                latest == null ? "Section conversation" : latest.getBody(),
                latest == null ? null : latest.getCreatedAt(),
                latest == null ? null : latest.getSenderType(), latest == null ? null : latest.getSenderCode(),
                readAt(type, code, key), unreadSection(type, code, sectionCode));
    }

    @Transactional
    public java.time.Instant markConversationRead(ParticipantType participantType, String participantCode,
                                                   String conversationType, ParticipantType otherType, String otherCode) {
        String key = switch (conversationType.toUpperCase(Locale.ROOT)) {
            case "DIRECT" -> {
                validateDirectParticipant(participantType, participantCode);
                validateDirectParticipant(otherType, otherCode);
                yield directKey(participantType, participantCode, otherType, otherCode);
            }
            case "SECTION" -> {
                String section = otherCode == null || otherCode.isBlank() ? "" : otherCode.trim();
                if (sectionRepository.findByCodeIgnoreCase(section).isEmpty()) throw notFound("Section", section);
                yield sectionKey(section);
            }
            case "GLOBAL" -> "global";
            default -> throw badRequest("Conversation type must be DIRECT, SECTION, or GLOBAL");
        };
        String normalizedCode = participantCode.trim();
        MessageReadState state = readStateRepository
                .findByParticipantTypeAndParticipantCodeIgnoreCaseAndConversationKey(
                        participantType, normalizedCode, key)
                .orElseGet(() -> new MessageReadState(participantType, normalizedCode, key, java.time.Instant.now()));
        java.time.Instant now = java.time.Instant.now();
        state.markReadAt(now);
        readStateRepository.save(state);
        return now;
    }

    private java.time.Instant readAt(ParticipantType type, String code, String key) {
        return readStateRepository.findByParticipantTypeAndParticipantCodeIgnoreCaseAndConversationKey(type, code, key)
                .map(MessageReadState::getLastReadAt).orElse(null);
    }

    private long unreadDirect(ParticipantType type, String code, ParticipantType peerType, String peerCode) {
        java.time.Instant after = readAt(type, code, directKey(type, code, peerType, peerCode));
        return messageRepository.countIncomingDirect(MessageAudience.DIRECT, peerType, peerCode, type, code,
                after == null ? java.time.Instant.EPOCH : after);
    }

    private long unreadSection(ParticipantType type, String code, String sectionCode) {
        java.time.Instant after = readAt(type, code, sectionKey(sectionCode));
        return messageRepository.countIncomingSection(MessageAudience.SECTION, sectionCode, type, code,
                after == null ? java.time.Instant.EPOCH : after);
    }

    private long unreadGlobal(ParticipantType type, String code) {
        java.time.Instant after = readAt(type, code, "global");
        return messageRepository.countIncomingGlobal(MessageAudience.GLOBAL, type, code,
                after == null ? java.time.Instant.EPOCH : after);
    }

    private String directKey(ParticipantType firstType, String firstCode, ParticipantType secondType, String secondCode) {
        return CampusMessageStream.directChannel(firstType, firstCode, secondType, secondCode);
    }

    private String sectionKey(String sectionCode) { return "section:" + sectionCode.trim().toLowerCase(Locale.ROOT); }
    private String contactName(ParticipantType type, String code) {
        if (type == ParticipantType.SYSTEM) return "Campus system";
        if (type == ParticipantType.TEACHER) return teacherRepository.findByStaffCodeIgnoreCase(code)
                .map(teacher -> teacher.getFullName() + " · " + teacher.getStaffCode()).orElse(code);
        if (type == ParticipantType.STUDENT) return studentRepository.findByStudentCodeIgnoreCase(code)
                .map(student -> student.getFullName() + " · " + student.getStudentCode()).orElse(code);
        return code;
    }

    public SseEmitter subscribeDirect(ParticipantType firstType, String firstCode,
                                      ParticipantType secondType, String secondCode) {
        validateDirectParticipant(firstType, firstCode);
        validateDirectParticipant(secondType, secondCode);
        return messageStream.subscribe(CampusMessageStream.directChannel(firstType, firstCode, secondType, secondCode));
    }

    public SseEmitter subscribeSection(String sectionCode) {
        String code = sectionCode.trim();
        if (sectionRepository.findByCodeIgnoreCase(code).isEmpty()) throw notFound("Section", code);
        return messageStream.subscribe("section:" + code.toLowerCase(Locale.ROOT));
    }

    public SseEmitter subscribeGlobal() { return messageStream.subscribe("global"); }
    @Transactional(readOnly = true)
    public List<CampusMessageResponse> directConversation(ParticipantType firstType, String firstCode,
                                                           ParticipantType secondType, String secondCode) {
        validateDirectParticipant(firstType, firstCode);
        validateDirectParticipant(secondType, secondCode);
        return chronological(messageRepository.findDirectConversation(MessageAudience.DIRECT,
                firstType, firstCode.trim(), secondType, secondCode.trim(), firstPage()));
    }

    @Transactional(readOnly = true)
    public List<CampusMessageResponse> sectionConversation(String sectionCode) {
        String code = sectionCode.trim();
        if (sectionRepository.findByCodeIgnoreCase(code).isEmpty()) throw notFound("Section", code);
        return chronological(messageRepository.findAllByAudienceAndRecipientCodeIgnoreCaseOrderByCreatedAtDesc(
                MessageAudience.SECTION, code, firstPage()));
    }

    @Transactional(readOnly = true)
    public List<CampusMessageResponse> globalFeed() {
        return chronological(messageRepository.findAllByAudienceOrderByCreatedAtDesc(MessageAudience.GLOBAL, firstPage()));
    }

    private void validateParticipants(SendMessageRequest request, String senderCode, String recipientCode) {
        if (request.senderType() == ParticipantType.SECTION) {
            throw badRequest("A section cannot send messages");
        }
        if (request.senderType() == ParticipantType.TEACHER
                && teacherRepository.findByStaffCodeIgnoreCase(senderCode).isEmpty()) {
            throw notFound("Teacher", senderCode);
        }
        if (request.senderType() == ParticipantType.STUDENT
                && studentRepository.findByStudentCodeIgnoreCase(senderCode).isEmpty()) {
            throw notFound("Student", senderCode);
        }
        switch (request.audience()) {
            case GLOBAL -> {
                if (request.recipientType() != null || recipientCode != null) {
                    throw badRequest("Global messages do not have a recipient; use audience GLOBAL");
                }
            }
            case SECTION -> {
                if (request.recipientType() != ParticipantType.SECTION || recipientCode == null || recipientCode.isBlank()) {
                    throw badRequest("Section messages require recipientType SECTION and a section code");
                }
                if (sectionRepository.findByCodeIgnoreCase(recipientCode).isEmpty()) throw notFound("Section", recipientCode);
            }
            case DIRECT -> {
                if (request.recipientType() == ParticipantType.SYSTEM) throw badRequest("Use @system to send a system request");
                if (request.recipientType() == null || request.recipientType() == ParticipantType.SECTION
                        || recipientCode == null || recipientCode.isBlank()) {
                    throw badRequest("Direct messages require a teacher or student recipient code");
                }
                if (request.recipientType() == ParticipantType.TEACHER
                        && teacherRepository.findByStaffCodeIgnoreCase(recipientCode).isEmpty()) {
                    throw notFound("Teacher", recipientCode);
                }
                if (request.recipientType() == ParticipantType.STUDENT
                        && studentRepository.findByStudentCodeIgnoreCase(recipientCode).isEmpty()) {
                    throw notFound("Student", recipientCode);
                }
            }
        }
    }

    private void validateDirectParticipant(ParticipantType type, String code) {
        if (type == ParticipantType.SYSTEM && "SYSTEM".equalsIgnoreCase(code)) return;
        if (type == null || type == ParticipantType.SECTION || type == ParticipantType.SYSTEM
                || code == null || code.isBlank()) {
            throw badRequest("A direct conversation requires valid teacher, student, or system participant codes");
        }
        if (type == ParticipantType.TEACHER && teacherRepository.findByStaffCodeIgnoreCase(code.trim()).isEmpty()) {
            throw notFound("Teacher", code);
        }
        if (type == ParticipantType.STUDENT && studentRepository.findByStudentCodeIgnoreCase(code.trim()).isEmpty()) {
            throw notFound("Student", code);
        }
    }

    private List<CampusMessageResponse> chronological(List<CampusMessage> messages) {
        List<CampusMessage> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);
        return ordered.stream().map(CampusMessageResponse::from).toList();
    }

    private Pageable firstPage() { return PageRequest.of(0, HISTORY_LIMIT); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String type, String value) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found: " + value);
    }

    public record ContactResponse(String conversationType, ParticipantType participantType, String code, String displayName, String lastMessage, java.time.Instant lastMessageAt, ParticipantType lastMessageSenderType, String lastMessageSenderCode, java.time.Instant lastReadAt, long unreadCount) { }

    public record SendMessageRequest(
            @NotNull ParticipantType senderType,
            @NotBlank @Size(max = 60) String senderCode,
            @NotNull MessageAudience audience,
            ParticipantType recipientType,
            @Size(max = 60) String recipientCode,
            @NotNull MessageCategory category,
            @NotBlank @Size(max = 2000) String body) { }
}
