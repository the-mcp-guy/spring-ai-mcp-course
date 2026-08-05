package com.themcpguy.supportdesk.orders.mcp;

import org.springframework.ai.mcp.annotation.McpToolParam;

/**
 * What Class 12 asks a person for before cancelling an order.
 *
 * <p>Spring AI generates a schema from this record and sends it with the elicitation
 * request, so the client knows what to ask for and what shape to send back.
 */
public record CancellationConfirmation(

        @McpToolParam(description = "Confirm this order should be cancelled", required = true)
        boolean confirmed,

        @McpToolParam(description = "Optional note recorded against the cancellation")
        String note) {
}
