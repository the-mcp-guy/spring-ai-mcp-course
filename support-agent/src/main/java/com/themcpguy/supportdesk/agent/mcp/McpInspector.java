package com.themcpguy.supportdesk.agent.mcp;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class McpInspector implements CommandLineRunner {

    private final List<McpSyncClient> clients;
    private final SyncMcpToolCallbackProvider toolCallbacks;

    McpInspector(List<McpSyncClient> clients, SyncMcpToolCallbackProvider toolCallbacks) {
        this.clients = clients;
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public void run(String... args) {
        for (McpSyncClient client : clients) {
            var info = client.getServerInfo();
            var capabilities = client.getServerCapabilities();
            System.out.printf("Connected to %s %s%n", info.name(), info.version());

            if (capabilities.tools() != null) {
                client.listTools().tools().forEach(tool ->
                        System.out.printf("  tool     %s%n", tool.name()));
            }
            if (capabilities.resources() != null) {
                client.listResources().resources().forEach(resource ->
                        System.out.printf("  resource %s%n", resource.uri()));

                client.listResourceTemplates().resourceTemplates()
                        .forEach(template -> System.out.printf("  template %s%n", template.uriTemplate()));
            }
            if (capabilities.prompts() != null) {
                client.listPrompts().prompts().forEach(prompt ->
                        System.out.printf("  prompt   %s%n", prompt.name()));
            }
        }

        var callbacks = toolCallbacks.getToolCallbacks();
        System.out.printf("The model is given %d tools:%n", callbacks.length);
        for (var callback : callbacks) {
            System.out.printf("  %s%n", callback.getToolDefinition().name());
        }
    }
}