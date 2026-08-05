package com.themcpguy.supportdesk.orders.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;

/**
 * The refund-email prompt, and completion for its order-ID argument.
 *
 * <p>The wording lives here rather than in every client that wants to send one, so
 * improving it improves all of them at once.
 */
@Component
public class RefundPrompts {

    private final OrderService orderService;

    RefundPrompts(OrderService orderService) {
        this.orderService = orderService;
    }

    @McpPrompt(
            name = "draft_refund_email",
            title = "Draft a refund email",
            description = "Write an email to a customer confirming a refund for one order.")
    public List<PromptMessage> draftRefundEmail(
            @McpArg(name = "orderId", description = "The order being refunded", required = true)
            String orderId,
            @McpArg(name = "reason", description = "Why the order is being refunded", required = true)
            String reason) {

        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));

        String instruction = """
                Write a short email to %s confirming a refund for order %s.

                Order total: %.2f
                Reason for the refund: %s

                Keep it to three sentences. Apologise once, state the amount, and say the
                money takes three to five working days to arrive. Do not invent a
                reference number or a date.
                """.formatted(order.customer().name(), order.orderId(),
                              order.totalAmount(), reason);

        // Role.USER on purpose. Returning a bare String would make it an ASSISTANT
        // message, which reads as though the model had already written the email.
        return List.of(new PromptMessage(Role.USER, new TextContent(instruction)));
    }

    @McpComplete(prompt = "draft_refund_email")
    public List<String> completeOrderId(String prefix) {
        return orderService.findIdsStartingWith(prefix, 20);
    }
}
