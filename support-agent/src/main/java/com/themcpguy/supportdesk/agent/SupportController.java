package com.themcpguy.supportdesk.agent;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * What the browser talks to.
 *
 * <p>Everything here also works from the command line; the browser is a convenience for
 * watching progress fill and answering a confirmation dialog.
 */
@RestController
public class SupportController {

    private final SupportAgentService agent;
    private final RefundEmailService refundEmails;
    private final BrowserChannel channel;
    private final BrowserConfirmationHandler confirmationHandler;
    private final ConversationContext conversations;

    SupportController(SupportAgentService agent,
                      RefundEmailService refundEmails,
                      BrowserChannel channel,
                      BrowserConfirmationHandler confirmationHandler,
                      ConversationContext conversations) {
        this.agent = agent;
        this.refundEmails = refundEmails;
        this.channel = channel;
        this.confirmationHandler = confirmationHandler;
        this.conversations = conversations;
    }

    public record ChatRequest(String conversationId, String message, String orderId) {}
    public record ChatReply(String reply) {}
    public record AnswerRequest(boolean confirmed, String note) {}
    public record RefundRequest(String orderId, String reason) {}

    @PostMapping("/api/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        conversations.set(request.conversationId());
        try {
            return new ChatReply(agent.chatWithPolicy(request.conversationId(), request.message()));
        }
        finally {
            conversations.clear();
        }
    }

    /** The stream progress, log messages and confirmation requests arrive on. */
    @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@RequestParam String conversationId) {
        return channel.open(conversationId);
    }

    /** Wakes the thread parked inside BrowserConfirmationHandler. */
    @PostMapping("/api/confirmations/{id}")
    public void answer(@PathVariable String id, @RequestBody AnswerRequest body) {
        confirmationHandler.answer(id, body.confirmed(), body.note());
    }

    @PostMapping("/api/refund-email")
    public ChatReply refundEmail(@RequestBody RefundRequest request) {
        return new ChatReply(refundEmails.draft(request.orderId(), request.reason()));
    }
}
