package com.themcpguy.supportdesk.orders.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.entity.OrderEntity;
import com.themcpguy.supportdesk.orders.entity.ShipmentEmbeddable;
import com.themcpguy.supportdesk.orders.repository.OrderRepository;

/**
 * Stands in for a carrier integration.
 *
 * <p>A real one would call each carrier's tracking API. This one takes a little time and
 * moves some estimates, which is enough for Class 11 to have a job worth reporting
 * progress on. Both which orders slip and how far they slip are derived from the order ID
 * and the shipping date, so a test gets the same answer every run, and so does a second
 * call on the same data.
 */
@Service
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final long lookupDelayMillis;

    ShipmentService(OrderRepository orderRepository,
                    @Value("${supportdesk.carrier-lookup-delay-ms:30}") long lookupDelayMillis) {
        this.orderRepository = orderRepository;
        this.lookupDelayMillis = lookupDelayMillis;
    }

    /**
     * Refresh one order's delivery estimate.
     *
     * @return true when the estimate moved
     */
    @Transactional
    public boolean refreshEstimate(Order order) {
        pauseAsThoughCallingACarrier();

        OrderEntity entity = orderRepository.findById(order.orderId()).orElse(null);
        if (entity == null || entity.getShipment() == null) {
            return false;
        }

        ShipmentEmbeddable shipment = entity.getShipment();
        LocalDate current = shipment.getEstimatedDelivery();
        if (current == null || order.shipment() == null) {
            return false;
        }

        LocalDate refreshed = recalculate(order.orderId(), current, order.shipment().shippedOn());

        if (refreshed.equals(current)) {
            return false;
        }

        shipment.setEstimatedDelivery(refreshed);
        orderRepository.save(entity);
        return true;
    }

    /**
     * Roughly one order in seven has slipped, to a date derived from the shipping date.
     *
     * <p>The shipping date never moves, so a second call on the same order arrives at the same
     * answer and leaves the estimate where it is. Deriving from the current estimate instead
     * would push the date further out on every call, which would make the tool's
     * idempotentHint a false claim.
     */
    private LocalDate recalculate(String orderId, LocalDate current, LocalDate shippedOn) {
        if (shippedOn == null) {
            return current;
        }
        int hash = Math.abs(orderId.hashCode());
        return hash % 7 == 0 ? shippedOn.plusDays(7 + hash % 2) : current;
    }

    private void pauseAsThoughCallingACarrier() {
        if (lookupDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(lookupDelayMillis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
