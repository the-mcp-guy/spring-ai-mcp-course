package com.themcpguy.supportdesk.agent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.spec.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Class 12: answers the server's confirmation request.
 *
 * <p>The hard part is that this method runs on a thread inside the agent while an MCP
 * tool call is still open on the server, and the answer has to come from a person on the
 * other end of an HTTP connection. So it sends the question out, blocks, and is woken by
 * a separate request carrying the answer.
 *
 * <p>Exactly one parameter, of type ElicitRequest. Spring AI rejects any other shape at
 * startup.
 */
@Component
@Profile("!cli")
public class BrowserConfirmationHandler {

    private static final Logger log = LoggerFactory.getLogger(BrowserConfirmationHandler.class);
    private static final long WAIT_SECONDS = 60;

    private final Map<String, SynchronousQueue<ElicitResult>> pending = new ConcurrentHashMap<>();
    private final BrowserChannel channel;
    private final ConversationContext conversations;

    BrowserConfirmationHandler(BrowserChannel channel, ConversationContext conversations) {
        this.channel = channel;
        this.conversations = conversations;
    }

    @McpElicitation(clients = "orders")
    public ElicitResult confirm(ElicitRequest request) {
        String conversationId = conversations.current();

        // Nobody is watching: the command line, or a browser that went away. Declining is
        // the safe answer, because the tool behind this cancels an order.
        if (conversationId == null || !channel.isWatching(conversationId)) {
            log.info("No one to ask, declining: {}", request.message());
            return new ElicitResult(ElicitResult.Action.DECLINE, Map.of());
        }

        String id = UUID.randomUUID().toString();
        SynchronousQueue<ElicitResult> slot = new SynchronousQueue<>();
        pending.put(id, slot);

        try {
            // ElicitRequest is an interface. The form variant carries the schema Spring
            // AI generated from CancellationConfirmation; a URL variant carries none.
            Object schema = request instanceof ElicitFormRequest form ? form.requestedSchema() : null;
            channel.ask(conversationId, id, request.message(), schema);

            ElicitResult answer = slot.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            return answer != null
                    ? answer
                    : new ElicitResult(ElicitResult.Action.CANCEL, Map.of());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ElicitResult(ElicitResult.Action.CANCEL, Map.of());
        }
        finally {
            pending.remove(id);
        }
    }

    /** Called by the controller when the browser posts an answer. */
    public void answer(String id, boolean confirmed, String note) {
        SynchronousQueue<ElicitResult> slot = pending.get(id);
        if (slot == null) {
            log.info("Answer for {} arrived too late", id);
            return;
        }
        slot.offer(confirmed
                ? new ElicitResult(ElicitResult.Action.ACCEPT,
                        Map.of("confirmed", true, "note", note == null ? "" : note))
                : new ElicitResult(ElicitResult.Action.DECLINE, Map.of()));
    }
}
