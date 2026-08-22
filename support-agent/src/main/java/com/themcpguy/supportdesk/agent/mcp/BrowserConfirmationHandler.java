package com.themcpguy.supportdesk.agent.mcp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.spec.McpSchema.ElicitFormRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitRequest;
import io.modelcontextprotocol.spec.McpSchema.ElicitResult;

import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.themcpguy.supportdesk.agent.service.BrowserChannel;

@Component
@Profile("!cli")
public class BrowserConfirmationHandler {

    /** How long the person has to answer. Everything else waits longer than this. */
    private static final long WAIT_SECONDS = 60;

    private final Map<String, SynchronousQueue<ElicitResult>> pending = new ConcurrentHashMap<>();
    private final BrowserChannel channel;

    BrowserConfirmationHandler(BrowserChannel channel) {
        this.channel = channel;
    }

    @McpElicitation(clients = "orders")
    public ElicitResult confirm(ElicitRequest request) {
        // The server put the conversation on the question, so it survives the hop to
        // whichever thread Spring AI runs this handler on.
        Object token = request.meta() == null ? null : request.meta().get("conversationId");
        String conversationId = token == null ? null : token.toString();

        // Nobody is watching: the command line, or a browser that went away.
        // Declining is the safe answer, because the tool behind this cancels an order.
        if (conversationId == null || !channel.isWatching(conversationId)) {
            return new ElicitResult(ElicitResult.Action.DECLINE, Map.of());
        }

        String id = UUID.randomUUID().toString();
        SynchronousQueue<ElicitResult> slot = new SynchronousQueue<>();
        pending.put(id, slot);

        try {
            Object schema = request instanceof ElicitFormRequest form
                    ? form.requestedSchema()
                    : null;
            channel.ask(conversationId, id, request.message(), schema, WAIT_SECONDS);

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
            return;
        }
        slot.offer(confirmed
                ? new ElicitResult(ElicitResult.Action.ACCEPT,
                Map.of("confirmed", true, "note", note == null ? "" : note))
                : new ElicitResult(ElicitResult.Action.DECLINE, Map.of()));
    }
}