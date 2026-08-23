package com.themcpguy.supportdesk.orders.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.domain.OrderStatus;
import com.themcpguy.supportdesk.orders.entity.OrderEntity;
import com.themcpguy.supportdesk.orders.repository.OrderRepository;

/**
 * Everything the application knows how to do with an order.
 *
 * <p>This class is not changed by any class in the course. The MCP layer added from
 * Class 2 onwards describes what is already here rather than reimplementing it.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> findById(String orderId) {
        return orderRepository.findById(orderId).map(OrderEntity::toDomain);
    }

    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(OrderEntity::toDomain)
                .toList();
    }

    public List<Order> findByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.parse(status))
                .stream()
                .map(OrderEntity::toDomain)
                .toList();
    }

    /** Backs the order-ID completion in Class 5. */
    public List<String> findIdsStartingWith(String prefix, int limit) {
        return orderRepository
                .findByOrderIdStartingWithOrderByOrderIdAsc(
                        prefix == null ? "" : prefix.toUpperCase(), Limit.of(limit))
                .stream()
                .map(OrderEntity::getOrderId)
                .toList();
    }

    @Transactional
    public Order updateStatus(String orderId, String newStatus) {
        OrderStatus status = OrderStatus.parse(newStatus);

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Use cancel_order to cancel an order. It asks the customer to confirm "
                            + "first and records the reason. This tool sets the other four statuses.");
        }

        OrderEntity order = require(orderId);

        order.setStatus(status);
        order.setLastUpdated(Instant.now());
        return orderRepository.save(order).toDomain();
    }

    /**
     * Cancel an order and record why. Class 12 puts a confirmation in front of this,
     * because it cannot be undone.
     */
    @Transactional
    public Order cancel(String orderId, String note) {
        OrderEntity order = require(orderId);

        if (!order.getStatus().isCancellable()) {
            throw new IllegalStateException(
                    "Order %s is %s and can no longer be cancelled. Only PENDING and PROCESSING orders can."
                            .formatted(orderId, order.getStatus()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationNote(note);
        order.setLastUpdated(Instant.now());
        return orderRepository.save(order).toDomain();
    }

    private OrderEntity require(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No order with ID '%s'. Check the ID and try again.".formatted(orderId)));
    }
}
