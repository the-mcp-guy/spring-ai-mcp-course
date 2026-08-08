package com.themcpguy.supportdesk.orders.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.service.OrderService;

@Component
public class RefundPrompts {

    private static final List<String> REFUND_REASONS = List.of(
            "arrived damaged", "arrived late", "faulty", "no longer needed", "wrong item");

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
                .orElseThrow(() -> new IllegalArgumentException("No order with ID " + orderId));

        String instruction = """
                Write a short email to %s confirming a refund for order %s.

                Order total: %.2f
                Reason for the refund: %s

                Keep it to three sentences. Apologise once, state the amount, and say the
                money takes three to five working days to arrive. Do not invent a
                reference number or a date.
                """.formatted(order.customer().name(), order.orderId(),
                order.totalAmount(), reason);

        return List.of(new PromptMessage(Role.USER, TextContent.builder(instruction).build()));
    }

    @McpComplete(prompt = "draft_refund_email")
    public McpSchema.CompleteResult completeRefundArgument(McpSchema.CompleteRequest.CompleteArgument argument) {
        return switch (argument.name()) {
            case "reason" -> completeReason(argument.value());
            case "orderId" -> completeOrderId(argument.value());
            default -> new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(List.of()));
        };
    }

    private McpSchema.CompleteResult completeReason(String typed) {
        List<String> matches = REFUND_REASONS.stream()
                .filter(reason -> reason.startsWith(typed))
                .toList();

        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(matches, matches.size(), false));
    }

    private McpSchema.CompleteResult completeOrderId(String typed) {
        List<String> all = orderService.findIdsStartingWith(typed, 21);
        boolean more = all.size() > 20;
        List<String> page = more ? all.subList(0, 20) : all;
        Integer total = more ? null : page.size();

        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(page, total, more));
    }
}