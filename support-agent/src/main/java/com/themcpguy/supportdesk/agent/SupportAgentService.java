package com.themcpguy.supportdesk.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;

/**
 * The agent. Class 7 builds it; Class 8 adds the resource-aware method.
 */
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
            - Quote amounts with two decimal places and the currency.
            - If a tool returns an error, tell the user what it said and what
              they could try instead.
            - The support team's notes come from two sources. Use knowledge_base_*
              tools for anything current. Use knowledge_base_archive_* tools only
              when the user asks how something worked in the past, or when an order
              predates 2024. If the two disagree, treat the current one as right,
              and say so.
            - Find a file before reading it: call the list_allowed_directories tool
              of whichever source you need, list that directory, then read the file.
            - If neither source covers the question, say so and offer to escalate to
              a team lead. Never invent policy.
            - Keep answers to a few sentences unless asked for detail.
            """;

    private final ChatClient chatClient;
    private final McpResources resources;

    SupportAgentService(ChatClient.Builder builder,
                        SyncMcpToolCallbackProvider mcpTools,
                        McpResources resources) {
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
                // The progress token is what a server's progress notifications are
                // addressed to. Using the conversation ID means the handler in Class 11
                // can route them straight back to the right browser.
                .toolContext(Map.of("progressToken", conversationId))
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Class 8: attach the returns policy before the model sees the question.
     *
     * <p>The order comes from a tool the model chooses to call. The policy comes from a
     * resource our code attached, because nearly every support conversation needs it.
     */
    public String chatWithPolicy(String conversationId, String userMessage) {
        String returnsPolicy = resources.read("policy://returns");

        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                // The progress token is what a server's progress notifications are
                // addressed to. Using the conversation ID means the handler in Class 11
                // can route them straight back to the right browser.
                .toolContext(Map.of("progressToken", conversationId))
                .system(system -> system.text("""
                        {base}

                        The current returns policy is below. Use it for any question about
                        returns, refunds or the returns window. Do not rely on anything you
                        remember about returns policies.

                        ---
                        {policy}
                        """).param("base", BASE_SYSTEM).param("policy", returnsPolicy))
                .user(userMessage)
                .call()
                .content();
    }
}
