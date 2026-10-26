package com.digitaltwin.dt.messages;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class CampusMessageController {
    private final CampusMessageService messageService;

    public CampusMessageController(CampusMessageService messageService) { this.messageService = messageService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampusMessageResponse send(@Valid @RequestBody CampusMessageService.SendMessageRequest request) {
        return messageService.send(request);
    }

    @GetMapping("/direct/{firstType}/{firstCode}/{secondType}/{secondCode}")
    public List<CampusMessageResponse> directConversation(@PathVariable ParticipantType firstType,
                                                           @PathVariable String firstCode,
                                                           @PathVariable ParticipantType secondType,
                                                           @PathVariable String secondCode) {
        return messageService.directConversation(firstType, firstCode, secondType, secondCode);
    }

    @GetMapping("/section/{sectionCode}")
    public List<CampusMessageResponse> sectionConversation(@PathVariable String sectionCode) {
        return messageService.sectionConversation(sectionCode);
    }

    @GetMapping(path = "/stream/direct/{firstType}/{firstCode}/{secondType}/{secondCode}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDirect(@PathVariable ParticipantType firstType, @PathVariable String firstCode,
                                   @PathVariable ParticipantType secondType, @PathVariable String secondCode) {
        return messageService.subscribeDirect(firstType, firstCode, secondType, secondCode);
    }

    @GetMapping(path = "/stream/section/{sectionCode}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSection(@PathVariable String sectionCode) {
        return messageService.subscribeSection(sectionCode);
    }

    @GetMapping(path = "/stream/global", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGlobal() { return messageService.subscribeGlobal(); }
    @GetMapping("/global")
    public List<CampusMessageResponse> globalFeed() { return messageService.globalFeed(); }
}
