package com.themcpguy.supportdesk.agent;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.Root;

import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.stereotype.Component;

/**
 * Class 13: client configuration that properties cannot express.
 *
 * <p>Called once per connection, with the connection name from application.yaml, so one
 * customizer configures each connection differently.
 */
@Component
public class SupportAgentClientCustomizer implements McpClientCustomizer<McpClient.SyncSpec> {

    @Override
    public void customize(String connectionName, McpClient.SyncSpec spec) {

        // The elicitation in Class 12 waits on a person, so the global 30s timeout is
        // far too short for this one connection.
        if ("orders".equals(connectionName)) {
            spec.requestTimeout(Duration.ofMinutes(5));
        }

        // Roots tell a server what we are working on. The filesystem server enforces its
        // own sandbox from its arguments; this is a statement of intent, not a permission.
        if (connectionName.startsWith("knowledge-base")) {
            String directory = connectionName.endsWith("archive") ? "support-kb-archive" : "support-kb";
            spec.roots(List.of(new Root(
                    new File(directory).getAbsoluteFile().toURI().toString(),
                    "Support knowledge base")));
        }
    }
}
