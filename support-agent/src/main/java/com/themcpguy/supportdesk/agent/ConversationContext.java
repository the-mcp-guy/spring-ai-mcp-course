package com.themcpguy.supportdesk.agent;

import org.springframework.stereotype.Component;

/**
 * Which conversation the current request belongs to.
 *
 * <p>An elicitation handler is called on the thread that is running the tool call, and
 * the {@code ElicitRequest} carries no conversation ID. Everything between the controller
 * and the handler is on one thread, so a thread-local is enough to bridge it.
 */
@Component
public class ConversationContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public void set(String conversationId) {
        CURRENT.set(conversationId);
    }

    public String current() {
        return CURRENT.get();
    }

    public void clear() {
        CURRENT.remove();
    }
}
