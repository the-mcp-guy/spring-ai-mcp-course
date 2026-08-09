package com.themcpguy.supportdesk.orders.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;

import java.util.List;

@Component
public class OrderTools {

    private final OrderService orderService;

    OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpTool(
            name = "get_order",
            generateOutputSchema = true,
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

    @McpTool(
            name = "get_customer_orders",
            description = """
            List every order belonging to one customer, newest first.
            Customer IDs look like CUST-42.
            Returns an empty list if the customer has no orders.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public List<Order> getCustomerOrders(
            @McpToolParam(description = "The customer ID, for example CUST-42") String customerId) {

        return orderService.findByCustomerId(customerId);
    }

    @McpTool(
            name = "get_orders_by_status",
            description = """
            List orders in one status, newest first.
            Valid statuses are PENDING, PROCESSING, SHIPPED, DELIVERED and CANCELLED.
            Returns an empty list if no order is in that status.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public List<Order> getOrdersByStatus(
            @McpToolParam(description = "One of PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED")
            String status) {

        return orderService.findByStatus(status);
    }

    @McpTool(
            name = "update_order_status",
            description = """
            Move an order to a new status.
            Valid statuses are PENDING, PROCESSING, SHIPPED, DELIVERED and CANCELLED.
            Returns the updated order.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public Order updateOrderStatus(
            @McpToolParam(description = "The order ID, for example ORD-10001") String orderId,
            @McpToolParam(description = "The new status") String newStatus) {

        return orderService.updateStatus(orderId, newStatus);
    }
}
