package com.digitaltwin.dt.messages;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CampusMessageStream {
    private final ConcurrentMap<String, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
        Set<SseEmitter> channelSubscribers = subscribers.computeIfAbsent(channel,
                ignored -> ConcurrentHashMap.newKeySet());
        channelSubscribers.add(emitter);
        Runnable remove = () -> remove(channel, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(ignored -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(channel));
        } catch (IOException ex) {
            remove.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @TransactionalEventListener
    public void publishAfterCommit(CampusMessageResponse message) {
        String channel = channelFor(message);
        Set<SseEmitter> channelSubscribers = subscribers.get(channel);
        if (channelSubscribers == null) return;
        for (SseEmitter emitter : channelSubscribers) {
            try {
                emitter.send(SseEmitter.event().name("message").id(message.id().toString()).data(message));
            } catch (IOException | IllegalStateException ex) {
                remove(channel, emitter);
                emitter.completeWithError(ex);
            }
        }
    }

    public static String directChannel(ParticipantType firstType, String firstCode,
                                       ParticipantType secondType, String secondCode) {
        String first = participantKey(firstType, firstCode);
        String second = participantKey(secondType, secondCode);
        return first.compareTo(second) <= 0 ? "direct:" + first + "|" + second : "direct:" + second + "|" + first;
    }

    private String channelFor(CampusMessageResponse message) {
        return switch (message.audience()) {
            case GLOBAL -> "global";
            case SECTION -> "section:" + message.recipientCode().toLowerCase(Locale.ROOT);
            case DIRECT -> directChannel(message.senderType(), message.senderCode(),
                    message.recipientType(), message.recipientCode());
        };
    }

    private static String participantKey(ParticipantType type, String code) {
        return type.name().toLowerCase(Locale.ROOT) + ":" + code.trim().toLowerCase(Locale.ROOT);
    }

    private void remove(String channel, SseEmitter emitter) {
        Set<SseEmitter> channelSubscribers = subscribers.get(channel);
        if (channelSubscribers == null) return;
        channelSubscribers.remove(emitter);
        if (channelSubscribers.isEmpty()) subscribers.remove(channel, channelSubscribers);
    }
}
