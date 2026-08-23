package com.themcpguy.supportdesk.agent.guard;

public class TokenBudgetExceededException extends RuntimeException {

    private final String conversationId;

    private final long spent;

    private final long budget;

    TokenBudgetExceededException(String conversationId, long spent, long budget) {
        super("Conversation %s has used %d tokens of its budget of %d"
                .formatted(conversationId, spent, budget));
        this.conversationId = conversationId;
        this.spent = spent;
        this.budget = budget;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public long getSpent() {
        return this.spent;
    }

    public long getBudget() {
        return this.budget;
    }
}