package com.themcpguy.supportdesk.orders.mcp;

// Spring Boot 4 ships Jackson 3, so this is tools.jackson, not com.fasterxml.jackson.
import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.service.OrderService;

/**
 * One order as a resource, addressed by a URI template.
 *
 * <p>The client fills the template in and sends the finished URI; the server matches it
 * back, which is why the method parameter has to be named after the placeholder.
 */
@Component
public class OrderResources {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    OrderResources(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @McpResource(
            uri = "order://{orderId}",
            name = "order",
            title = "Order",
            description = "A single order, as JSON. The orderId looks like ORD-10001.",
            mimeType = "application/json")
    public String order(String orderId) {
        return orderService.findById(orderId)
                .map(this::toJson)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not serialise the order", e);
        }
    }
}
