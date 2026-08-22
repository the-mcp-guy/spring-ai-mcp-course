package com.themcpguy.supportdesk.agent;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

import org.springframework.stereotype.Component;

/**
 * Reading resources and running prompts, which the tool callback bridge does not cover.
 *
 * <p>{@code SyncMcpToolCallbackProvider} exists so the <em>model</em> can invoke tools.
 * Resources are attached by the application and prompts are chosen by a person, so
 * neither belongs in the list of things the model may call. Both go through the client
 * directly.
 */
@Component
public class McpResources {

    private final List<McpSyncClient> clients;

    McpResources(List<McpSyncClient> clients) {
        this.clients = clients;
    }

    public String read(String uri) {
        return orders().readResource(new ReadResourceRequest(uri))
                .contents()
                .stream()
                .filter(TextResourceContents.class::isInstance)
                .map(TextResourceContents.class::cast)
                .map(TextResourceContents::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No text contents at " + uri));
    }

    public String orderResource(String orderId) {
        return read("order://" + orderId);
    }

    /** Class 5's template, filled in by the client. */
    public String order(String orderId) {
        return read("order://" + orderId);
    }

    /**
     * With more than one connection, picking the first is no longer good enough. The
     * order server is the one that reports itself as order-service.
     */
    McpSyncClient orders() {
        return clients.stream()
                .filter(client -> "order-service".equals(client.getServerInfo().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Not connected to order-service. Is it running on port 8080?"));
    }
}
