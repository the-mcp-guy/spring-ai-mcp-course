package com.themcpguy.supportdesk.orders.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.domain.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the starter's business logic.
 *
 * <p>None of this is taught in the course. It is here so that a change to the seed data
 * or the service is caught before it reaches a lesson that quotes the numbers.
 */
@SpringBootTest
@Transactional   // rolls back after each test, so the cancel below does not leak into the others
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Test
    void findsTheOrderTheCourseQuotes() {
        Order order = orderService.findById("ORD-10001").orElseThrow();

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.customer().name()).isEqualTo("Ana Ruiz");
        assertThat(order.customer().email()).isEqualTo("ana.ruiz@example.com");
        assertThat(order.totalAmount()).isEqualByComparingTo("179.99");
        assertThat(order.items()).hasSize(2);
        assertThat(order.shipment().carrier()).isEqualTo("DHL");
        assertThat(order.shipment().trackingNumber()).isEqualTo("DHL-88213");
    }

    @Test
    void theOrderClassTwelveCancelsIsPending() {
        Order order = orderService.findById("ORD-10002").orElseThrow();

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.status().isCancellable()).isTrue();
        assertThat(order.customer().name()).isEqualTo("Marcus Adeyemi");
        assertThat(order.totalAmount()).isEqualByComparingTo("34.99");
    }

    @Test
    void unknownIdsAreEmptyRatherThanAnException() {
        assertThat(orderService.findById("ORD-99999")).isEmpty();
    }

    @Test
    void seedsTheStatusSpreadTheCourseQuotes() {
        assertThat(orderService.findByStatus("SHIPPED")).hasSize(87);
        assertThat(orderService.findByStatus("DELIVERED")).hasSize(50);
        assertThat(orderService.findByStatus("PENDING")).hasSize(30);
        assertThat(orderService.findByStatus("PROCESSING")).hasSize(25);
        assertThat(orderService.findByStatus("CANCELLED")).hasSize(8);
    }

    @Test
    void anInvalidStatusNamesTheValidOnes() {
        assertThatThrownBy(() -> orderService.findByStatus("IN_TRANSIT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IN_TRANSIT")
                .hasMessageContaining("PENDING")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void listsACustomersOrdersNewestFirst() {
        List<Order> orders = orderService.findByCustomerId("CUST-42");

        assertThat(orders).isNotEmpty();
        assertThat(orders).extracting(Order::orderId).contains("ORD-10001", "ORD-10003");
        assertThat(orders).isSortedAccordingTo(
                (a, b) -> b.createdAt().compareTo(a.createdAt()));
    }

    @Test
    void unknownCustomersGetAnEmptyListRatherThanAnError() {
        assertThat(orderService.findByCustomerId("CUST-99")).isEmpty();
    }

    @Test
    void completesOrderIdsByPrefix() {
        List<String> ids = orderService.findIdsStartingWith("ORD-1000", 20);

        assertThat(ids).contains("ORD-10001", "ORD-10002", "ORD-10003");
        assertThat(ids).allMatch(id -> id.startsWith("ORD-1000"));
    }

    @Test
    void refusesToCancelAShippedOrder() {
        assertThatThrownBy(() -> orderService.cancel("ORD-10001", "changed their mind"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHIPPED")
                .hasMessageContaining("can no longer be cancelled");
    }

    @Test
    void cancelsAPendingOrderAndKeepsTheNote() {
        Order cancelled = orderService.cancel("ORD-10002", "arrived damaged");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderService.findById("ORD-10002").orElseThrow().status())
                .isEqualTo(OrderStatus.CANCELLED);
    }
}
