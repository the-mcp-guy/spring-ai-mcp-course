package com.themcpguy.supportdesk.orders.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RefundPromptsTest {

    @Autowired
    RefundPrompts prompts;

    @Test
    void shouldPutTheCustomerDetailsInTheRefundPrompt() {
        List<PromptMessage> messages = prompts.draftRefundEmail("ORD-10001", "arrived damaged");

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().role()).isEqualTo(Role.USER);
        assertThat(((TextContent) messages.getFirst().content()).text())
                .contains("Ana Ruiz")
                .contains("ORD-10001")
                .contains("179.99");
    }
}
