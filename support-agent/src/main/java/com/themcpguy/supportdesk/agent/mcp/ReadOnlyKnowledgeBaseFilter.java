package com.themcpguy.supportdesk.agent.mcp;

import java.util.Set;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class ReadOnlyKnowledgeBaseFilter implements McpToolFilter {

    private static final Set<String> ALLOWED_FILE_TOOLS =
            Set.of("read_text_file", "list_directory", "list_allowed_directories");

    @Override
    public boolean test(McpConnectionInfo connectionInfo, Tool tool) {
        String serverName = connectionInfo.initializeResult() != null
                ? connectionInfo.initializeResult().serverInfo().name()
                : null;

        if ("order-service".equals(serverName)) {
            return true;
        }
        return ALLOWED_FILE_TOOLS.contains(tool.name());
    }
}