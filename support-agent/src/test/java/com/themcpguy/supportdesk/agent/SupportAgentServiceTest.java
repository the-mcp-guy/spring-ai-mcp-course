package com.themcpguy.supportdesk.agent;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Class 14, client side: that the agent is wired up and asks the model.
 *
 * <p>The MCP client is disabled in src/test/resources/application.yaml, so no connection
 * is opened and the suite needs neither order-service running nor Node installed. With
 * the client off, SyncMcpToolCallbackProvider is never created and something has to
 * supply it, which is the trap the class describes.
 */
@SpringBootTest
@Import(SupportAgentServiceTest.Stubs.class)
class SupportAgentServiceTest {

    /**
     * Both stand-ins have to be right before the context is built, because
     * ChatClient.Builder reads them while SupportAgentService is being constructed.
     * A plain @MockitoBean is stubbed too late and fails in two different ways:
     *
     * <ul>
     *   <li>A mocked SyncMcpToolCallbackProvider returns null from getToolCallbacks(),
     *       Mockito's default for an array, and defaultTools() fails with
     *       "Cannot read the array length because callbacks is null".
     *   <li>A mocked ChatModel returns null from getOptions(), and the builder fails with
     *       "Cannot invoke ChatOptions.mutate() because getOptions() is null".
     * </ul>
     */
    @TestConfiguration
    static class Stubs {

        @Bean
        SyncMcpToolCallbackProvider syncMcpToolCallbackProvider() {
            return new SyncMcpToolCallbackProvider(List.of());
        }

        // @Primary because the Anthropic starter is on the test classpath and
        // autoconfigures its own ChatModel, leaving two candidates.
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

    /**
     * The mock is a singleton bean rather than a @MockitoBean, so nothing resets it
     * between tests and recorded invocations accumulate.
     */
    @BeforeEach
    void resetTheModel() {
        reset(chatModel);
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());
    }

    @Test
    void sendsTheQuestionToTheModel() {
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
    void attachesTheReturnsPolicyToTheSystemPrompt() {
        given(resources.read("policy://returns")).willReturn("RETURNS POLICY BODY");
        given(chatModel.call(any(Prompt.class)))
                .willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));

        agent.chatWithPolicy("test", "Can they still return it?", null);

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());

        // The policy is in the conversation before the model sees the question, which is
        // the difference between a resource and a tool.
        assertThat(prompt.getValue().getInstructions())
                .anyMatch(message -> message.getText().contains("RETURNS POLICY BODY"));
    }
}
