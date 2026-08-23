package com.themcpguy.supportdesk.orders.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.domain.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderToolsTest {

    @Autowired
    OrderTools tools;

    @Test
    void shouldReturnTheRequestedOrder() {
        Order order = tools.getOrder("ORD-10001", new McpMeta(null));

        assertThat(order.orderId()).isEqualTo("ORD-10001");
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.items()).hasSize(2);
    }

    @Test
    void shouldExplainAnUnknownOrderId() {
        assertThatThrownBy(() -> tools.getOrder("ORD-99999", new McpMeta(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORD-99999")
                .hasMessageContaining("Check the ID");
    }
}