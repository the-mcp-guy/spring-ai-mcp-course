package com.themcpguy.supportdesk.orders.mcp;

import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.service.OrderService;

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
                .map(objectMapper::writeValueAsString)
                .orElseThrow(() -> new IllegalArgumentException("No order with ID " + orderId));
    }
}