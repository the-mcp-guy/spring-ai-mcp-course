package com.themcpguy.supportdesk.agent.cli;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.agent.service.SupportAgentService;

@Component
@Profile("cli")   // so it does not read standard input when serving the browser
public class SupportCli implements CommandLineRunner {

    private final SupportAgentService agent;

    SupportCli(SupportAgentService agent) {
        this.agent = agent;
    }

    @Override
    public void run(String... args) {
        System.out.println("Support desk ready. Ask about an order. Type 'quit' to exit.\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();

                if (input.isBlank()) {
                    continue;
                }
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    System.out.println("Agent: " + agent.chat("cli", input));
//                    System.out.print("Agent: ");
//                    agent.chatStream("cli", input)
//                            .doOnNext(System.out::print)
//                            .blockLast();
//                    System.out.println();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }
}