package com.themcpguy.supportdesk.agent.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class BrowserChannel {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter open(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L);   // no timeout; the browser closes it
        emitter.onCompletion(() -> emitters.remove(conversationId, emitter));
        emitter.onTimeout(() -> emitters.remove(conversationId, emitter));
        emitter.onError(e -> emitters.remove(conversationId, emitter));
        emitters.put(conversationId, emitter);
        return emitter;
    }

    public void progress(String conversationId, int percent) {
        send(conversationId, "progress", Map.of("percent", percent));
    }

    private void send(String conversationId, String event, Object payload) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter == null) {
            return;   // nobody is watching, which is normal on the command line
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(payload));
        }
        catch (IOException | IllegalStateException e) {
            emitters.remove(conversationId, emitter);
        }
    }

    public void ask(String conversationId, String id, String message, Object schema, long seconds) {
        send(conversationId, "confirmation", Map.of("id", id, "message", message, "seconds", seconds));
    }

    public boolean isWatching(String conversationId) {
        return emitters.containsKey(conversationId);
    }
}