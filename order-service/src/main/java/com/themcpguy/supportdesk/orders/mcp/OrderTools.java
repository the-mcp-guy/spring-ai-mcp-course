package com.themcpguy.supportdesk.orders.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;

@Component
public class OrderTools {

    private final OrderService orderService;

    OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpTool(
            name = "get_order",
            description = """
            Look up a single order by its ID.
            Returns the status, the customer, the line items, the total and the shipment.
            Order IDs look like ORD-10001.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public Order getOrder(
            @McpToolParam(description = "The order ID, for example ORD-10001") String orderId) {

        return orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));
    }
}