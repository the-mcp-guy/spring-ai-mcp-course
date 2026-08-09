        package com.themcpguy.supportdesk.agent.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import com.themcpguy.supportdesk.agent.service.SupportAgentService;

@RestController
public class SupportController {

    private final SupportAgentService agent;

    SupportController(SupportAgentService agent) {
        this.agent = agent;
    }

    public record ChatRequest(String conversationId, String message) {}
    public record ChatReply(String reply) {}

    @PostMapping("/api/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        try {
            return new ChatReply(agent.chat(request.conversationId(), request.message()));
        } catch (ResourceAccessException e) {
            return new ChatReply("The model did not answer in time, so the request was stopped. Ask again in a moment.");
        }
    }
}