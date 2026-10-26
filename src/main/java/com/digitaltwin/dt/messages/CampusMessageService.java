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

    public CampusMessageService(CampusMessageRepository messageRepository, TeacherRepository teacherRepository,
                                SectionRepository sectionRepository, CampusStudentRepository studentRepository,
                                CampusMessageStream messageStream, ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.messageStream = messageStream;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CampusMessageResponse send(SendMessageRequest request) {
        String senderCode = request.senderCode().trim();
        String recipientCode = request.recipientCode() == null ? null : request.recipientCode().trim();
        validateParticipants(request, senderCode, recipientCode);
        CampusMessage saved = messageRepository.save(new CampusMessage(request.senderType(), senderCode,
                request.audience(), request.recipientType(), recipientCode, request.category(), request.body().trim()));
        CampusMessageResponse response = CampusMessageResponse.from(saved);
        eventPublisher.publishEvent(response);
        return response;
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
        if (type == null || type == ParticipantType.SECTION || code == null || code.isBlank()) {
            throw badRequest("A direct conversation requires teacher or student participant codes");
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

    public record SendMessageRequest(
            @NotNull ParticipantType senderType,
            @NotBlank @Size(max = 60) String senderCode,
            @NotNull MessageAudience audience,
            ParticipantType recipientType,
            @Size(max = 60) String recipientCode,
            @NotNull MessageCategory category,
            @NotBlank @Size(max = 2000) String body) { }
}
