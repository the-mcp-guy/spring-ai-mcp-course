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

import com.themcpguy.supportdesk.orders.domain.OrderStatus;
import com.themcpguy.supportdesk.orders.service.OrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class OrderToolsContextTest {

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
    void shouldReportProgressAndFinishTheRecheck() {
        String summary = tools.recheckShipments(context);

        assertThat(summary).contains("Rechecked 87 shipments");
        verify(context).progress(100);
        verify(context).info("Rechecking 87 shipments");
    }

    @Test
    void shouldLeaveTheOrderPendingWhenTheUserDeclines() {
        elicitReturns(new StructuredElicitResult<>(Action.DECLINE, null, Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("The client declined the confirmation");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldLeaveTheOrderPendingWhenAcceptedWithoutConfirming() {
        elicitReturns(new StructuredElicitResult<>(Action.ACCEPT,
                new CancellationConfirmation(false, "changed my mind"), Map.of()));

        String result = tools.cancelOrder(context, "ORD-10002");

        assertThat(result).contains("confirmation was declined");
        assertThat(statusOf("ORD-10002")).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldRefuseAShippedOrderWithoutAsking() {
        String result = tools.cancelOrder(context, "ORD-10001");

        assertThat(result).contains("can no longer be cancelled");
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
