package com.themcpguy.supportdesk.agent.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.themcpguy.supportdesk.agent.mcp.McpResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(SupportAgentServiceTest.Stubs.class)
class SupportAgentServiceTest {

    @TestConfiguration
    static class Stubs {

        @Bean
        SyncMcpToolCallbackProvider syncMcpToolCallbackProvider() {
            return new SyncMcpToolCallbackProvider(List.of());
        }

        // @Primary because the Anthropic starter autoconfigures its own ChatModel,
        // leaving two candidates.
        @Bean
        @Primary
        ChatModel chatModel() {
            ChatModel model = mock(ChatModel.class);
            given(model.getOptions()).willReturn(ChatOptions.builder().build());
            return model;
        }
    }

    @Autowired
    ChatModel chatModel;

    @MockitoBean
    McpResources resources;

    @Autowired
    SupportAgentService agent;

    @BeforeEach
    void resetTheModel() {
        reset(chatModel);
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());
    }

    @Test
    void shouldSendTheQuestionToTheModel() {
        given(chatModel.call(any(Prompt.class)))
                .willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        String reply = agent.chat("test", "Where is ORD-10001?");

        assertThat(reply).isEqualTo("ok");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions())
                .anyMatch(message -> message.getText().contains("Where is ORD-10001?"));
    }

    @Test
    void shouldAttachTheReturnsPolicyToTheSystemPrompt() {
        given(resources.read("policy://returns")).willReturn("RETURNS POLICY BODY");
        given(chatModel.call(any(Prompt.class)))
                .willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        agent.chatWithPolicy("test", "Can they still return it?", null);

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());

        assertThat(prompt.getValue().getInstructions())
                .anyMatch(message -> message.getText().contains("RETURNS POLICY BODY"));
    }
}
