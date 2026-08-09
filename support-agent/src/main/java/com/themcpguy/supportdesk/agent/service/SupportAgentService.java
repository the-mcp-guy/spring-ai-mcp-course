package com.themcpguy.supportdesk.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SupportAgentService {

    private final ChatClient chatClient;

    SupportAgentService(ChatClient.Builder builder, SyncMcpToolCallbackProvider mcpTools) {
        this.chatClient = builder
                .defaultSystem("""
                    You are a support agent for an online shop. You answer questions
                    about orders using the tools you have been given.

                    Guidelines:
                    - Use a tool to find out anything about an order. Never guess an
                      order ID, a status, a total or a delivery date.
                    - Order IDs look like ORD-10001 and customer IDs like CUST-42. If
                      the user gives you something that is not in that form, ask.
                    - Amounts are in euros. Quote them with two decimal places and the euro sign.
                    - If a tool returns an error, tell the user what it said and what
                      they could try instead.
                    - Keep answers to a few sentences unless asked for detail.
                    """)
                .defaultTools(mcpTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder().build()).build())
                .build();
    }

    public String chat(String conversationId, String userMessage) {
        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(userMessage)
                .call()
                .content();
    }

    public Flux<String> chatStream(String conversationId, String userMessage) {
        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(userMessage)
                .stream()
                .content();
    }
}