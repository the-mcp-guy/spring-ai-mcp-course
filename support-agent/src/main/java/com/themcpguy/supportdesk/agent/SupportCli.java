package com.themcpguy.supportdesk.agent;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Class 7: the same agent, from a terminal.
 *
 * <p>Behind the "cli" profile so it does not read standard input when the application is
 * serving the browser:
 *
 * <pre>mvn -pl support-agent spring-boot:run -Dspring-boot.run.profiles=cli</pre>
 *
 * <p>Confirmations behave differently here. Class 12's handler declines when no browser
 * is watching, so asking the agent to cancel an order from the command line reports that
 * it was not cancelled. That is the safe answer rather than a missing feature.
 */
@Component
@Profile("cli")
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
                }
                catch (Exception e) {
                    // Without this the first failure ends the session, which is tiresome
                    // while experimenting.
                    System.err.println("Error: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }
}
