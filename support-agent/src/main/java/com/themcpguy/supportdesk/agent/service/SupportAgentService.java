package com.themcpguy.supportdesk.agent.service;

import com.themcpguy.supportdesk.agent.mcp.McpResources;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

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
                    - The support team's notes come from two sources, and which one applies
                      depends on the order. Before answering any question about a specific order,
                      call get_order and look at when it was placed. An order placed before 2024
                      was handled under the archived policies: use knowledge_base_archive_* tools
                      for it, and say that the rules you are quoting are the ones that applied at
                      the time. For an order from 2024 onwards, use knowledge_base_* tools. If the
                      two disagree, the current notes are right for a current order.
                    - Find a file before reading it: call the list_allowed_directories tool of
                      whichever source you need, list that directory, then read the file.
                    - If neither source covers the question, say so and offer to escalate to a
                      team lead. Never invent policy.
                    - Some tools ask the user to confirm before they act. When the user asks for
                      such an action, call the tool and let it ask its own question. Do not ask
                      the user to confirm beforehand.
                    """;

    /**
     * Who is using the app. There is no login yet (Class 17 looks at authorization),
     * so the operating-system username stands in for one.
     */
    static final String AGENT_ID = System.getProperty("user.name");

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
                .toolContext(Map.of("progressToken", conversationId, "agentId", AGENT_ID))
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
                .toolContext(Map.of("progressToken", conversationId, "agentId", AGENT_ID))
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
                .toolContext(Map.of("progressToken", conversationId, "agentId", AGENT_ID))
                .user(userMessage)
                .stream()
                .content();
    }
}