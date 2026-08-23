package com.themcpguy.supportdesk.agent.mcp;

import java.util.List;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

import org.springframework.stereotype.Component;

@Component
public class McpResources {

    private final List<McpSyncClient> clients;

    McpResources(List<McpSyncClient> clients) {
        this.clients = clients;
    }

    public McpSyncClient orders() {
        return clients.stream()
                .filter(client -> "order-service".equals(client.getServerInfo().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Not connected to order-service. Is it running on port 8080?"));
    }

    public String read(String uri) {
        return orders().readResource(ReadResourceRequest.builder(uri).build())
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
}