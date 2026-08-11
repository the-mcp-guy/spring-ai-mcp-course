package com.themcpguy.supportdesk.agent.mcp;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class McpInspector implements CommandLineRunner {

    private final List<McpSyncClient> clients;

    McpInspector(List<McpSyncClient> clients) {
        this.clients = clients;
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
    }
}