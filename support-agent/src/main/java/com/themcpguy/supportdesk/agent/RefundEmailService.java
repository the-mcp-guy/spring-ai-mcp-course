package com.themcpguy.supportdesk.agent;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * Class 8: run the server's prompt and hand its messages to the model.
 *
 * <p>No tools. Drafting an email from a prompt that already carries the order details
 * needs none, and leaving them out means the model cannot decide to go looking.
 */
@Service
public class RefundEmailService {

    private final McpResources resources;
    private final ChatClient chatClient;

    RefundEmailService(McpResources resources, ChatClient.Builder builder) {
        this.resources = resources;
        this.chatClient = builder.build();
    }

    public String draft(String orderId, String reason) {
        var result = resources.orders().getPrompt(new GetPromptRequest(
                "draft_refund_email",
                Map.of("orderId", orderId, "reason", reason)));

        List<Message> messages = result.messages().stream()
                .map(RefundEmailService::toSpringAiMessage)
                .toList();

        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }

    /**
     * MCP has its own PromptMessage and Role; Spring AI has UserMessage and
     * AssistantMessage. Nothing maps them, so this does.
     */
    private static Message toSpringAiMessage(PromptMessage promptMessage) {
        String text = promptMessage.content() instanceof TextContent textContent
                ? textContent.text()
                : promptMessage.content().toString();

        return promptMessage.role() == Role.USER
                ? new UserMessage(text)
                : new AssistantMessage(text);
    }
}
