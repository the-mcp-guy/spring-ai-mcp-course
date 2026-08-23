package com.themcpguy.supportdesk.orders.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PolicyResources {

    @McpResource(
            uri = "policy://returns",
            name = "returns_policy",
            title = "Returns policy",
            description = "The current returns policy, including the returns window and exclusions.",
            mimeType = "text/markdown")
    public String returnsPolicy() {
        return read("policies/returns.md");
    }

    @McpResource(
            uri = "policy://shipping",
            name = "shipping_policy",
            title = "Shipping policy",
            description = "Carriers, delivery estimates and what happens to a delayed shipment.",
            mimeType = "text/markdown")
    public String shippingPolicy() {
        return read("policies/shipping.md");
    }

    private String read(String path) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}