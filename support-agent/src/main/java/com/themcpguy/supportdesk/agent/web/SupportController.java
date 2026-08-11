package com.themcpguy.supportdesk.agent.web;

import com.themcpguy.supportdesk.agent.service.RefundEmailService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import com.themcpguy.supportdesk.agent.service.SupportAgentService;

@RestController
public class SupportController {

    private final SupportAgentService agent;
    private final RefundEmailService refundEmails;

    SupportController(SupportAgentService agent, RefundEmailService refundEmails) {
        this.agent = agent;
        this.refundEmails = refundEmails;
    }

    public record RefundRequest(String orderId, String reason) {}
    public record ChatRequest(String conversationId, String message, String orderId) {}
    public record ChatReply(String reply) {}

    @PostMapping("/api/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        try {
            return new ChatReply(agent.chatWithPolicy(
                    request.conversationId(), request.message(), request.orderId()));
        } catch (ResourceAccessException e) {
            return new ChatReply("The model did not answer in time, so the request was stopped. Ask again in a moment.");
        }
    }

    @PostMapping("/api/refund-email")
    public ChatReply refundEmail(@RequestBody RefundRequest request) {
        return new ChatReply(refundEmails.draft(request.orderId(), request.reason()));
    }
}
