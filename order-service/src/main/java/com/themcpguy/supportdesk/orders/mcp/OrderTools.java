package com.themcpguy.supportdesk.orders.mcp;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;
import com.themcpguy.supportdesk.orders.service.ShipmentService;

/**
 * The order tools, as MCP sees them.
 *
 * <p>Nothing here implements any business logic. Each method describes something
 * {@link OrderService} already does, in the terms the protocol uses, and the descriptions
 * and error messages are written for the model that reads them.
 */
@Component
public class OrderTools {

    private final OrderService orderService;
    private final ShipmentService shipmentService;

    OrderTools(OrderService orderService, ShipmentService shipmentService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
    }

    // ── Class 2 ──────────────────────────────────────────────────────────────

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

    // ── Class 3 ──────────────────────────────────────────────────────────────

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

    // ── Class 11 ─────────────────────────────────────────────────────────────

    @McpTool(
            name = "recheck_shipments",
            description = """
            Refresh the delivery estimate for every order that has shipped but not
            arrived. Takes a while. Returns a summary of what changed.
            Use this when the user asks to update or refresh delivery dates in bulk.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true))
    public String recheckShipments(McpSyncRequestContext context) {

        List<Order> inTransit = orderService.findByStatus("SHIPPED");
        context.info("Rechecking %d shipments".formatted(inTransit.size()));

        int changed = 0;
        for (int i = 0; i < inTransit.size(); i++) {
            Order order = inTransit.get(i);

            if (shipmentService.refreshEstimate(order)) {
                changed++;
                context.debug("Updated estimate for %s".formatted(order.orderId()));
            }

            context.progress((i + 1) * 100 / inTransit.size());
        }

        context.info("Finished. %d estimates changed.".formatted(changed));
        return "Rechecked %d shipments, %d estimates changed.".formatted(inTransit.size(), changed);
    }

    // ── Class 12 ─────────────────────────────────────────────────────────────

    @McpTool(
            name = "cancel_order",
            description = """
            Cancel an order and start a refund. The user is asked to confirm before
            anything changes. Only PENDING and PROCESSING orders can be cancelled.
            """,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = true,
                    idempotentHint = false,
                    openWorldHint = false))
    public String cancelOrder(
            McpSyncRequestContext context,
            @McpToolParam(description = "The order ID, for example ORD-10002") String orderId) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));

        if (!order.status().isCancellable()) {
            return "Order %s is %s and can no longer be cancelled. Only PENDING and PROCESSING orders can."
                    .formatted(orderId, order.status());
        }

        if (!context.elicitEnabled()) {
            return "This client cannot ask for confirmation, and cancelling needs it. "
                    + "Cancel %s through the admin console instead.".formatted(orderId);
        }

        var answer = context.elicit(
                spec -> spec.message("Cancel order %s for %s? The total is %.2f and a refund will be started."
                        .formatted(orderId, order.customer().name(), order.totalAmount())),
                CancellationConfirmation.class);

        return switch (answer.action()) {
            case ACCEPT -> {
                if (answer.structuredContent() == null || !answer.structuredContent().confirmed()) {
                    yield "Order %s was not cancelled: the confirmation was declined.".formatted(orderId);
                }
                orderService.cancel(orderId, answer.structuredContent().note());
                context.info("Cancelled %s".formatted(orderId));
                yield "Order %s is cancelled and a refund of %.2f has been started."
                        .formatted(orderId, order.totalAmount());
            }
            case DECLINE -> "Order %s was not cancelled: the user said no.".formatted(orderId);
            case CANCEL -> "Order %s was not cancelled: the user dismissed the question.".formatted(orderId);
        };
    }
}
