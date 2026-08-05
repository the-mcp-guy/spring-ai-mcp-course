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
 * progress on. The decision is derived from the order ID rather than randomly, so a test
 * gets the same answer every run.
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
        LocalDate refreshed = recalculate(order.orderId(), current);

        if (refreshed.equals(current)) {
            return false;
        }

        shipment.setEstimatedDelivery(refreshed);
        orderRepository.save(entity);
        return true;
    }

    /** Roughly one order in seven slips by a day or two. */
    private LocalDate recalculate(String orderId, LocalDate current) {
        if (current == null) {
            return null;
        }
        int hash = Math.abs(orderId.hashCode());
        return hash % 7 == 0 ? current.plusDays(1 + hash % 2) : current;
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
