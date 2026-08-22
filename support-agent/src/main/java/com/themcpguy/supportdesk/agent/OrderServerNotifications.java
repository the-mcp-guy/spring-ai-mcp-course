package com.themcpguy.supportdesk.agent;

import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

/**
 * Class 11: what the order server says while a tool is still running.
 *
 * <p>Both signatures are fixed. Each handler takes either one parameter or three, and
 * nothing else, or Spring AI rejects it at startup. The three-parameter progress form is
 * (Double progress, String progressToken, String total): the value first, the token
 * second, and total as a String.
 */
@Component
public class OrderServerNotifications {

    private static final Logger log = LoggerFactory.getLogger(OrderServerNotifications.class);

    private final BrowserChannel channel;

    OrderServerNotifications(BrowserChannel channel) {
        this.channel = channel;
    }

    @McpProgress(clients = "orders")
    public void onProgress(ProgressNotification notification) {
        // The server calls context.progress(int) with 0-100, but that sends
        // percentage / 100.0 with total 1.0, so what arrives here is a fraction.
        Object token = notification.progressToken();
        int percent = (int) Math.round(notification.progress() * 100);
        log.info("  [{}] {}%", token, percent);

        // The agent sets the progress token to the conversation ID, so it routes.
        if (token != null) {
            channel.progress(token.toString(), percent);
        }
    }

    @McpLogging(clients = "orders")
    public void onLog(LoggingMessageNotification notification) {
        log.info("  {} {}", notification.level(), notification.data());
    }
}
