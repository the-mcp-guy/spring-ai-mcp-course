package com.themcpguy.supportdesk.agent;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Class 6: prints what arrived over the connections at startup.
 *
 * <p>Kept in the finished application because it is the quickest way to see whether a
 * server is reachable and what the naming and filtering did to its tools.
 */
@Component
public class McpInspector implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(McpInspector.class);

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
            log.info("Connected to {} {}", info.name(), info.version());

            // Ask only for what the server said it has. Calling listResources() on a
            // server that does not advertise resources raises
            // IllegalStateException: Server does not provide the resources capability.
            if (capabilities.tools() != null) {
                client.listTools().tools().forEach(tool -> log.info("  tool     {}", tool.name()));
            }
            if (capabilities.resources() != null) {
                client.listResources().resources().forEach(r -> log.info("  resource {}", r.uri()));
                client.listResourceTemplates().resourceTemplates()
                        .forEach(t -> log.info("  template {}", t.uriTemplate()));
            }
            if (capabilities.prompts() != null) {
                client.listPrompts().prompts().forEach(p -> log.info("  prompt   {}", p.name()));
            }
        }

        // What the model actually receives, after the filter and the prefix generator.
        var callbacks = toolCallbacks.getToolCallbacks();
        log.info("The model is given {} tools:", callbacks.length);
        for (var callback : callbacks) {
            log.info("  {}", callback.getToolDefinition().name());
        }
    }
}
