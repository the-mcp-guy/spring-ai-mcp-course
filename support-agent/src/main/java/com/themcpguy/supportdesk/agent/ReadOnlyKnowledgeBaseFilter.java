package com.themcpguy.supportdesk.agent;

import java.util.Set;

import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.stereotype.Component;

/**
 * Class 10: keeps the filesystem write tools away from the model.
 *
 * <p>This is not a security boundary. It keeps tools out of the model's sight; it does
 * not stop anything else calling them, and it does not stop the server being able to do
 * them. An agent that must not write belongs on a directory it cannot write to.
 */
@Component
public class ReadOnlyKnowledgeBaseFilter implements McpToolFilter {

    private static final Set<String> ALLOWED_FILE_TOOLS =
            Set.of("read_text_file", "read_file", "list_directory", "search_files",
                    "directory_tree", "list_allowed_directories");

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
