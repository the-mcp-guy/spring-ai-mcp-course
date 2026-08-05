package com.themcpguy.supportdesk.orders.mcp;

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.ElicitResult.Action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.ai.mcp.annotation.context.StructuredElicitResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.domain.OrderStatus;
import com.themcpguy.supportdesk.orders.service.OrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class 14: the tool methods as plain Java.
 *
 * <p>No MCP protocol and no model. The context the two bidirectional tools take is an
 * interface, so a mock supplies it and decides the outcome.
 */
@SpringBootTest
@Transactional
class OrderToolsTest {

    @Autowired
    OrderTools tools;

    @Autowired
    OrderService orderService;

    private McpSyncRequestContext context;

    @BeforeEach
    void setUp() {
        context = mock(McpSyncRequestContext.class);
    }

    @Test
    void getOrderReturnsTheOrder() {
        Order order = tools.getOrder("ORD-10001");

        assertThat(order.orderId()).isEqualTo("ORD-10001");
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.items()).hasSize(2);
    }

    @Test
    void getOrderExplainsAnUnknownId() {
        assertThatThrownBy(() -> tools.getOrder("ORD-99999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORD-99999")
                .hasMessageContaining("Check the ID");
    }

    @Test
    void invalidStatusNamesTheValidOnes() {
        assertThatThrownBy(() -> tools.getOrdersByStatus("IN_TRANSIT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IN_TRANSIT")
                .hasMessageContaining("SHIPPED");
    }

    @Test
    void recheckReportsProgressAndFinishes() {
        String summary = tools.recheckShipments(context);

        assertThat(summary).contains("Rechecked 87 shipments");
        verify(context).progress(100);
        verify(context).info("Rechecking 87 shipments");
    }

    // ── Class 12: the four ways cancelling can end ───────────────────────────

    @Test
    void cancelDoesNothingWhenTheClientCannotAsk() {
        when(context.elicitEnabled()).thenReturn(false);

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("admin console");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
        verify(context, never()).elicit(any(), eq(CancellationConfirmation.class));
    }

    @Test
    void cancelDoesNothingWhenTheUserDeclines() {
        elicitReturns(new StructuredElicitResult<>(Action.DECLINE, null, Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("the user said no");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void cancelDoesNothingWhenTheUserDismissesTheQuestion() {
        elicitReturns(new StructuredElicitResult<>(Action.CANCEL, null, Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("dismissed the question");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void acceptedButNotConfirmedIsStillANo() {
        elicitReturns(new StructuredElicitResult<>(Action.ACCEPT,
                new CancellationConfirmation(false, "changed my mind"), Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("confirmation was declined");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void confirmingCancelsTheOrder() {
        elicitReturns(new StructuredElicitResult<>(Action.ACCEPT,
                new CancellationConfirmation(true, "arrived damaged"), Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("is cancelled");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void aShippedOrderIsRefusedBeforeAnyoneIsAsked() {
        String result = tools.cancelOrder(context, "ORD-10001");

        assertThat(result).contains("SHIPPED").contains("can no longer be cancelled");
        verify(context, never()).elicitEnabled();
    }

    private void elicitReturns(StructuredElicitResult<CancellationConfirmation> result) {
        when(context.elicitEnabled()).thenReturn(true);
        when(context.elicit(any(), eq(CancellationConfirmation.class))).thenReturn(result);
    }

    private OrderStatus statusOf(String orderId) {
        return orderService.findById(orderId).orElseThrow().status();
    }
}
