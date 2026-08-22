package com.themcpguy.supportdesk.agent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The open channel to the browser.
 *
 * <p>Progress notifications (Class 11), log messages (Class 11) and confirmation requests
 * (Class 12) all arrive on a thread inside the agent and have to reach a person. This
 * holds one SSE connection per conversation and pushes to it.
 */
@Component
public class BrowserChannel {

    private static final Logger log = LoggerFactory.getLogger(BrowserChannel.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter open(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L);   // no timeout; the browser closes it
        emitter.onCompletion(() -> emitters.remove(conversationId, emitter));
        emitter.onTimeout(() -> emitters.remove(conversationId, emitter));
        emitter.onError(e -> emitters.remove(conversationId, emitter));
        emitters.put(conversationId, emitter);
        return emitter;
    }

    public void ask(String conversationId, String id, String message, Object schema) {
        send(conversationId, "confirmation", Map.of("id", id, "message", message));
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
            log.debug("Dropping {} for {}: {}", event, conversationId, e.getMessage());
            emitters.remove(conversationId, emitter);
        }
    }

    /**
     * The conversation a tool call belongs to.
     *
     * <p>MCP notifications carry a progress token rather than our conversation ID, so
     * something has to bridge them. The agent sets the token to the conversation ID when
     * it makes the request, which keeps this simple.
     */
    public boolean isWatching(String conversationId) {
        return emitters.containsKey(conversationId);
    }
}
