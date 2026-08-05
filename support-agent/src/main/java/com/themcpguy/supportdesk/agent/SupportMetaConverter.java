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

    @Override
    public Map<String, Object> convert(ToolContext toolContext) {
        Map<String, Object> meta = new HashMap<>();
        if (toolContext == null || toolContext.getContext() == null) {
            return meta;
        }

        Object agentId = toolContext.getContext().get("agentId");
        if (agentId != null) {
            meta.put("support_agent_id", agentId);
        }
        return meta;
    }
}
