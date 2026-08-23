package com.themcpguy.supportdesk.agent.web;

import com.themcpguy.supportdesk.agent.guard.TokenBudgetExceededException;
import com.themcpguy.supportdesk.agent.mcp.BrowserConfirmationHandler;
import com.themcpguy.supportdesk.agent.service.BrowserChannel;
import com.themcpguy.supportdesk.agent.service.RefundEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

import com.themcpguy.supportdesk.agent.service.SupportAgentService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    private final SupportAgentService agent;
    private final RefundEmailService refundEmails;
    private final BrowserChannel channel;
    private final BrowserConfirmationHandler confirmationHandler;

    SupportController(SupportAgentService agent, RefundEmailService refundEmails,
                      BrowserChannel channel, BrowserConfirmationHandler confirmationHandler) {
        this.agent = agent;
        this.refundEmails = refundEmails;
        this.channel = channel;
        this.confirmationHandler = confirmationHandler;
    }

    public record RefundRequest(String orderId, String reason) {}
    public record ChatRequest(String conversationId, String message, String orderId) {}
    public record ChatReply(String reply) {}
    public record AnswerRequest(boolean confirmed, String note) {}

    /** Wakes the thread parked inside BrowserConfirmationHandler. */
    @PostMapping("/api/confirmations/{id}")
    public void answer(@PathVariable String id, @RequestBody AnswerRequest body) {
        confirmationHandler.answer(id, body.confirmed(), body.note());
    }

    @PostMapping("/api/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        try {
            return new ChatReply(agent.chatWithPolicy(
                    request.conversationId(), request.message(), request.orderId()));
        } catch (ResourceAccessException e) {
            return new ChatReply("The model did not answer in time, so the request was stopped. Ask again in a moment.");
        } catch (TokenBudgetExceededException e) {
            log.warn("Conversation {} used {} tokens of its budget of {}",
                    e.getConversationId(), e.getSpent(), e.getBudget());
            return new ChatReply("This conversation has reached its limit. Please start a new one.");
        } catch (Exception e) {
            log.error("Chat request failed", e);
            return new ChatReply("Something went wrong. Please try again later.");
        }
    }

    @PostMapping("/api/refund-email")
    public ChatReply refundEmail(@RequestBody RefundRequest request) {
        return new ChatReply(refundEmails.draft(request.orderId(), request.reason()));
    }

    /** Opens the event stream for one conversation. The browser calls this when the page loads. */
    @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@RequestParam String conversationId) {
        return channel.open(conversationId);
    }

}
