package com.themcpguy.supportdesk.orders.mcp;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.service.OrderService;

@Component
public class OrderResources {

    private final OrderService orderService;

    OrderResources(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpResource(
            uri = "order://{orderId}",
            name = "order",
            title = "Order",
            description = "A single order, as JSON. The orderId looks like ORD-10001.",
            mimeType = "application/json")
    public String order(String orderId) {
        return orderService.findById(orderId)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("No order with ID " + orderId));
    }
}