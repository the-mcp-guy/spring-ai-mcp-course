package com.themcpguy.supportdesk.agent.config;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.stereotype.Component;

@Component
public class ServerNamePrefixGenerator implements McpToolNamePrefixGenerator {

    @Override
    public String prefixedToolName(McpConnectionInfo connectionInfo, Tool tool) {
        String serverName = connectionInfo.initializeResult() != null
                ? connectionInfo.initializeResult().serverInfo().name()
                : null;

        if ("order-service".equals(serverName)) {
            return tool.name();
        }

        return connectionName(connectionInfo) + "_" + tool.name();
    }

    private static String connectionName(McpConnectionInfo connectionInfo) {
        // "support-agent - knowledge-base-archive": the client name and the connection
        // name, with a separator. Only the second half identifies the connection.
        String raw = connectionInfo.clientInfo().name();
        int separator = raw.lastIndexOf(" - ");
        String connection = separator < 0 ? raw : raw.substring(separator + 3);

        return connection.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}