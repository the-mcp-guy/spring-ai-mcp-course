package com.themcpguy.supportdesk.orders.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.themcpguy.supportdesk.orders.domain.Order;
import com.themcpguy.supportdesk.orders.domain.OrderStatus;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItemEmbeddable> items = new ArrayList<>();

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Embedded
    private ShipmentEmbeddable shipment;

    @Column(name = "cancellation_note")
    private String cancellationNote;

    protected OrderEntity() {
        // for JPA
    }

    public OrderEntity(String orderId, OrderStatus status, CustomerEntity customer,
                       List<OrderItemEmbeddable> items, BigDecimal totalAmount,
                       Instant createdAt, Instant lastUpdated, ShipmentEmbeddable shipment) {
        this.orderId = orderId;
        this.status = status;
        this.customer = customer;
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.shipment = shipment;
    }

    public Order toDomain() {
        return new Order(
                orderId,
                status,
                customer == null ? null : customer.toDomain(),
                items.stream().map(OrderItemEmbeddable::toDomain).toList(),
                totalAmount,
                createdAt,
                lastUpdated,
                shipment == null ? null : shipment.toDomain());
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ShipmentEmbeddable getShipment() {
        return shipment;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setCancellationNote(String cancellationNote) {
        this.cancellationNote = cancellationNote;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }
}
