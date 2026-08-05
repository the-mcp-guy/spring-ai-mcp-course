package com.themcpguy.supportdesk.agent;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.stereotype.Component;

/**
 * Class 10: names that say where a tool came from.
 *
 * <p>Without this bean, DefaultMcpToolNamePrefixGenerator renames the second
 * read_text_file to alt_1_read_text_file, which is unique and tells the model nothing.
 *
 * <p>The prefix is taken from the <em>connection</em> name rather than serverInfo,
 * because both knowledge-base servers are the same npm package started twice and report
 * themselves identically.
 */
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

    /**
     * The connection name, as an identifier.
     *
     * <p>clientInfo().name() is "support-agent - knowledge-base-archive": the client name
     * and the connection name with a separator. Only the second half identifies the
     * connection, and a tool name may not contain spaces, so both need dealing with.
     */
    private static String connectionName(McpConnectionInfo connectionInfo) {
        String raw = connectionInfo.clientInfo().name();
        int separator = raw.lastIndexOf(" - ");
        String connection = separator < 0 ? raw : raw.substring(separator + 3);

        return connection.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
