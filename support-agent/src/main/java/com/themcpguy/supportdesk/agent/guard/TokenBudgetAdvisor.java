package com.themcpguy.supportdesk.agent.guard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.NullMarked;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

@NullMarked
public class TokenBudgetAdvisor implements CallAdvisor {

    private final long budgetPerConversation;

    private final Map<String, AtomicLong> spent = new ConcurrentHashMap<>();

    TokenBudgetAdvisor(long budgetPerConversation) {
        this.budgetPerConversation = budgetPerConversation;
    }

    @Override
    public String getName() {
        return "tokenBudget";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String conversationId = conversationIdOf(request);
        AtomicLong spentHere = this.spent.computeIfAbsent(conversationId, id -> new AtomicLong());

        long alreadySpent = spentHere.get();
        if (alreadySpent >= this.budgetPerConversation) {
            throw new TokenBudgetExceededException(conversationId, alreadySpent, this.budgetPerConversation);
        }

        ChatClientResponse response = chain.nextCall(request);

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null) {
            spentHere.addAndGet(chatResponse.getMetadata().getUsage().getTotalTokens());
        }
        return response;
    }

    private String conversationIdOf(ChatClientRequest request) {
        Object conversationId = request.context().get(ChatMemory.CONVERSATION_ID);
        return (conversationId != null) ? conversationId.toString() : "unknown";
    }
}