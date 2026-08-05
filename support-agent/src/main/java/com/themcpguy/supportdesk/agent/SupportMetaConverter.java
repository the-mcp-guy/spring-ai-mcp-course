package com.themcpguy.supportdesk.agent;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.stereotype.Component;

/**
 * Class 13: application context the model should not see.
 *
 * <p>Which support agent is asking belongs in the audit log and not in a tool argument,
 * because a tool argument is in the schema and the model can read and set it. MCP's
 * _meta field carries it instead.
 */
@Component
public class SupportMetaConverter implements ToolContextToMcpMetaConverter {

    /**
     * The key MCP reads a progress token from. {@code McpSchema.Request.progressToken()}
     * is a default method that looks it up in {@code _meta} under exactly this name.
     */
    private static final String PROGRESS_TOKEN = "progressToken";

    @Override
    public Map<String, Object> convert(ToolContext toolContext) {
        Map<String, Object> meta = new HashMap<>();
        if (toolContext == null || toolContext.getContext() == null) {
            return meta;
        }

        // Pass the progress token through. Replacing the default converter and forgetting
        // this stops every progress notification silently: the server calls
        // context.progress(...), the request carries no token, and nothing is delivered.
        Object progressToken = toolContext.getContext().get(PROGRESS_TOKEN);
        if (progressToken != null) {
            meta.put(PROGRESS_TOKEN, progressToken);
        }

        Object agentId = toolContext.getContext().get("agentId");
        if (agentId != null) {
            meta.put("support_agent_id", agentId);
        }
        return meta;
    }
}
