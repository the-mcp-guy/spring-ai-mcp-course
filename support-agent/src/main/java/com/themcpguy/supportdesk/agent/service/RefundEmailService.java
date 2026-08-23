package com.themcpguy.supportdesk.agent.service;

import java.util.List;
import java.util.Map;

import com.themcpguy.supportdesk.agent.mcp.McpResources;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class RefundEmailService {

    private final McpResources resources;
    private final ChatClient chatClient;

    RefundEmailService(McpResources resources, ChatClient.Builder builder) {
        this.resources = resources;
        this.chatClient = builder.build();
    }

    public String draft(String orderId, String reason) {
        var result = resources.orders().getPrompt(GetPromptRequest.builder("draft_refund_email")
                .arguments(Map.of("orderId", orderId, "reason", reason))
                .build());

        List<Message> messages = result.messages().stream()
                .map(RefundEmailService::toSpringAiMessage)
                .toList();

        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }

    private static Message toSpringAiMessage(PromptMessage promptMessage) {
        String text = promptMessage.content() instanceof TextContent textContent
                ? textContent.text()
                : promptMessage.content().toString();

        return promptMessage.role() == Role.USER
                ? new UserMessage(text)
                : new AssistantMessage(text);
    }
}