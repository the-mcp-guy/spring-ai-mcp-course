package com.themcpguy.supportdesk.agent.mcp;

import com.themcpguy.supportdesk.agent.service.BrowserChannel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;

import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

@Component
public class OrderServerNotifications {

    private final BrowserChannel channel;

    OrderServerNotifications(BrowserChannel channel) {
        this.channel = channel;
    }

    @McpProgress(clients = "orders")
    public void onProgress(ProgressNotification notification) {
        Object token = notification.progressToken();
        int percent = (int) Math.round(notification.progress() * 100);
        System.out.printf("  [%s] %d%%%n", token, percent);

        if (token != null) {
            channel.progress(token.toString(), percent);
        }
    }

    @McpLogging(clients = "orders")
    public void onLog(LoggingMessageNotification notification) {
        System.out.printf("  %s %s%n", notification.level(), notification.data());
    }
}