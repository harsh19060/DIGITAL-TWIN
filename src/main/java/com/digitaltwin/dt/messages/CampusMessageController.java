package com.digitaltwin.dt.messages;

import com.digitaltwin.dt.session.CampusIdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class CampusMessageController {
    private final CampusMessageService messageService;
    private final CampusIdentityService identityService;

    public CampusMessageController(CampusMessageService messageService, CampusIdentityService identityService) {
        this.messageService = messageService;
        this.identityService = identityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusMessageResponse send(@Valid @RequestBody CampusMessageService.SendMessageRequest request,
                                     Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        if (request.senderType() != identity.participantType()
                || !identity.participantCode().equalsIgnoreCase(request.senderCode().trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only send messages as your signed-in account");
        }
        if (identity.participantType() == ParticipantType.STUDENT && request.audience() == MessageAudience.SECTION) {
            boolean ownSection = messageService.contacts(identity.participantType(), identity.participantCode()).stream()
                    .anyMatch(contact -> contact.conversationType().equals("SECTION")
                            && contact.code().equalsIgnoreCase(request.recipientCode()));
            if (!ownSection) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Students can only post in their own section chat");
        }
        return messageService.send(request);
    }

    @GetMapping("/contacts")
    public List<CampusMessageService.ContactResponse> contacts(Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        return messageService.contacts(identity.participantType(), identity.participantCode());
    }

    @PostMapping("/read")
    public java.time.Instant markRead(@Valid @RequestBody ReadRequest request, Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        String type = request.conversationType().trim().toUpperCase(java.util.Locale.ROOT);
        if ("SECTION".equals(type)) assertSectionAccess(request.code(), authentication);
        if ("DIRECT".equals(type) && (request.participantType() == null || request.code() == null || request.code().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direct read receipts require a participant type and code");
        }
        return messageService.markConversationRead(identity.participantType(), identity.participantCode(), type,
                request.participantType(), request.code());
    }
    @GetMapping("/direct/{firstType}/{firstCode}/{secondType}/{secondCode}")
    public List<CampusMessageResponse> directConversation(@PathVariable ParticipantType firstType,
                                                           @PathVariable String firstCode,
                                                           @PathVariable ParticipantType secondType,
                                                           @PathVariable String secondCode,
                                                           Authentication authentication) {
        assertConversationParticipant(firstType, firstCode, secondType, secondCode, authentication);
        return messageService.directConversation(firstType, firstCode, secondType, secondCode);
    }

    @GetMapping("/section/{sectionCode}")
    public List<CampusMessageResponse> sectionConversation(@PathVariable String sectionCode,
                                                           Authentication authentication) {
        assertSectionAccess(sectionCode, authentication);
        return messageService.sectionConversation(sectionCode);
    }

    @GetMapping(path = "/stream/direct/{firstType}/{firstCode}/{secondType}/{secondCode}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDirect(@PathVariable ParticipantType firstType, @PathVariable String firstCode,
                                   @PathVariable ParticipantType secondType, @PathVariable String secondCode,
                                   Authentication authentication) {
        assertConversationParticipant(firstType, firstCode, secondType, secondCode, authentication);
        return messageService.subscribeDirect(firstType, firstCode, secondType, secondCode);
    }

    @GetMapping(path = "/stream/section/{sectionCode}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSection(@PathVariable String sectionCode, Authentication authentication) {
        assertSectionAccess(sectionCode, authentication);
        return messageService.subscribeSection(sectionCode);
    }

    @GetMapping(path = "/stream/global", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGlobal() { return messageService.subscribeGlobal(); }

    @GetMapping("/global")
    public List<CampusMessageResponse> globalFeed() { return messageService.globalFeed(); }

    public record ReadRequest(@jakarta.validation.constraints.NotBlank String conversationType,
                              ParticipantType participantType,
                              @jakarta.validation.constraints.Size(max = 60) String code) { }
    private void assertConversationParticipant(ParticipantType firstType, String firstCode,
                                               ParticipantType secondType, String secondCode,
                                               Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        boolean firstIsUser = firstType == identity.participantType()
                && identity.participantCode().equalsIgnoreCase(firstCode);
        boolean secondIsUser = secondType == identity.participantType()
                && identity.participantCode().equalsIgnoreCase(secondCode);
        if (!firstIsUser && !secondIsUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only open conversations that include your signed-in account");
        }
    }

    private void assertSectionAccess(String sectionCode, Authentication authentication) {
        var identity = identityService.requireParticipant(authentication);
        if (identity.participantType() == ParticipantType.STUDENT) {
            boolean allowed = messageService.contacts(identity.participantType(), identity.participantCode()).stream()
                    .anyMatch(contact -> contact.conversationType().equals("SECTION")
                            && contact.code().equalsIgnoreCase(sectionCode));
            if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only open your own section chat");
        }
    }
}
