package com.themcpguy.supportdesk.agent.mcp;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class SupportMetaConverter implements ToolContextToMcpMetaConverter {

    /**
     * The MCP SDK looks a progress token up in {@code _meta} under exactly this
     * name, so the key has to match it character for character.
     */
    private static final String PROGRESS_TOKEN = "progressToken";

    @Override
    public Map<String, Object> convert(ToolContext toolContext) {
        Map<String, Object> meta = new HashMap<>();

        // Pass the progress token through. Without a token the server cannot send
        // progress notifications: it logs "Progress notification not supported by
        // the client!" and the Class 11 progress bar stays empty.
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