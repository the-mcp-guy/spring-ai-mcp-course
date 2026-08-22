package com.themcpguy.supportdesk.agent.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.annotation.McpPromptListChanged;
import org.springframework.ai.mcp.annotation.McpResourceListChanged;
import org.springframework.ai.mcp.annotation.McpToolListChanged;
import org.springframework.stereotype.Component;

@Component
public class CapabilityChanges {

    @McpToolListChanged(clients = "orders")
    public void toolsChanged(List<McpSchema.Tool> tools) {
        System.out.printf("order-service now offers %d tools%n", tools.size());
    }

    @McpResourceListChanged(clients = "orders")
    public void resourcesChanged(List<McpSchema.Resource> resources) {
        System.out.printf("order-service now offers %d resources%n", resources.size());
    }

    @McpPromptListChanged(clients = "orders")
    public void promptsChanged(List<McpSchema.Prompt> prompts) {
        System.out.printf("order-service now offers %d prompts%n", prompts.size());
    }
}