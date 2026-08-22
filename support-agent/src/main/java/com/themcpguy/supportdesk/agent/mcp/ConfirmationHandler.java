package com.themcpguy.supportdesk.agent.mcp;

import java.util.Map;
import java.util.Scanner;

import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;

import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
public class ConfirmationHandler {

    @McpElicitation(clients = "orders")
    public ElicitResult confirm(ElicitRequest request) {
        System.out.println();
        System.out.println(request.message());
        System.out.print("Type 'yes' to confirm: ");

        String answer = new Scanner(System.in).nextLine().trim();

        if (answer.equalsIgnoreCase("yes")) {
            return new ElicitResult(ElicitResult.Action.ACCEPT,
                    Map.of("confirmed", true, "note", "confirmed at the command line"));
        }
        return new ElicitResult(ElicitResult.Action.DECLINE, Map.of());
    }
}