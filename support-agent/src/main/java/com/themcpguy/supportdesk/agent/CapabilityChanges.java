package com.themcpguy.supportdesk.agent;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpPromptListChanged;
import org.springframework.ai.mcp.annotation.McpResourceListChanged;
import org.springframework.ai.mcp.annotation.McpToolListChanged;
import org.springframework.stereotype.Component;

/**
 * Class 13: reacting when a server's capabilities move.
 *
 * <p>The agent keeps working without these, because SyncMcpToolCallbackProvider resolves
 * tools per request rather than caching them at startup. These are for doing something
 * about the change.
 *
 * <p>Each handler takes exactly one List parameter of the matching type.
 */
@Component
public class CapabilityChanges {

    private static final Logger log = LoggerFactory.getLogger(CapabilityChanges.class);

    @McpToolListChanged(clients = "orders")
    public void toolsChanged(List<McpSchema.Tool> tools) {
        log.info("order-service now offers {} tools", tools.size());
    }

    @McpResourceListChanged(clients = "orders")
    public void resourcesChanged(List<McpSchema.Resource> resources) {
        log.info("order-service now offers {} resources", resources.size());
    }

    @McpPromptListChanged(clients = "orders")
    public void promptsChanged(List<McpSchema.Prompt> prompts) {
        log.info("order-service now offers {} prompts", prompts.size());
    }
}
