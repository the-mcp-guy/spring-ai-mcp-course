package com.themcpguy.supportdesk.agent.service;

import com.themcpguy.supportdesk.agent.mcp.McpResources;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SupportAgentService {

    static final String BASE_SYSTEM = """
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
                    - The support team's own notes are files you can read with the filesystem
                      tools. Use them for carrier delays and claims windows, for escalation, and
                      for the refund procedure. Call list_allowed_directories to find the
                      knowledge base, list it to see what is there, then read the file you need.
                    """;

    private final ChatClient chatClient;
    private final McpResources resources;

    SupportAgentService(ChatClient.Builder builder, SyncMcpToolCallbackProvider mcpTools, McpResources resources) {
        this.resources = resources;
        this.chatClient = builder
                .defaultSystem(BASE_SYSTEM)
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

    public String chatWithPolicy(String conversationId, String userMessage, String orderId) {
        String returnsPolicy = resources.read("policy://returns");
        String orderBlock = orderId == null || orderId.isBlank() ? "" : """

                The conversation was opened from the order below. Questions about "this
                order" or "the customer" refer to it.

                """ + resources.orderResource(orderId);

        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(system -> system.text("""
                    {base}

                    The current returns policy is below. Use it for any question about
                    returns, refunds or the returns window. Do not rely on anything you
                    remember about returns policies.

                    ---
                    {policy}
                    {order}
                    """)
                        .param("base", BASE_SYSTEM)
                        .param("policy", returnsPolicy)
                        .param("order", orderBlock))
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