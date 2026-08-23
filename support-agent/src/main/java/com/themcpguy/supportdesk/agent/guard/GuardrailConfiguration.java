package com.themcpguy.supportdesk.agent.guard;

import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(GuardrailProperties.class)
class GuardrailConfiguration {

    @Bean
    SafeGuardAdvisor safeGuardAdvisor(GuardrailProperties properties) {
        return SafeGuardAdvisor.builder()
                .sensitiveWords(properties.blockedWords())
                .failureResponse(properties.refusalMessage())
                .order(Ordered.HIGHEST_PRECEDENCE + 10)
                .build();
    }

    @Bean
    TokenBudgetAdvisor tokenBudgetAdvisor(GuardrailProperties properties) {
        return new TokenBudgetAdvisor(properties.tokensPerConversation());
    }
}