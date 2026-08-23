package com.themcpguy.supportdesk.agent.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Root;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class SupportAgentClientCustomizer implements McpClientCustomizer<McpClient.SyncSpec> {

    @Override
    public void customize(String connectionName, McpClient.SyncSpec spec) {
        if ("knowledge-base".equals(connectionName)) {
            spec.capabilities(ClientCapabilities.builder().roots(true).build());
            spec.roots(List.of(new Root(
                    Path.of("support-kb").toAbsolutePath().toUri().toString(),
                    "Support knowledge base")));
        }

        if ("knowledge-base-archive".equals(connectionName)) {
            spec.capabilities(ClientCapabilities.builder().roots(true).build());
            spec.roots(List.of(new Root(
                    Path.of("support-kb-archive").toAbsolutePath().toUri().toString(),
                    "Archived support knowledge base")));
        }

        if ("orders".equals(connectionName)) {
            spec.requestTimeout(Duration.ofMinutes(2));
        }
    }
}
