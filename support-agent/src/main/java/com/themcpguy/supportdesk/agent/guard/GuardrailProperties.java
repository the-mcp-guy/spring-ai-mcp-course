package com.themcpguy.supportdesk.agent.guard;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supportdesk.guardrails")
record GuardrailProperties(List<String> blockedWords, String refusalMessage,
                           long tokensPerConversation) {
}