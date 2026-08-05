package com.themcpguy.supportdesk.orders.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.themcpguy.supportdesk.orders.domain.Shipment;

@Embeddable
public class ShipmentEmbeddable {

    private String carrier;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "shipped_on")
    private LocalDate shippedOn;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    protected ShipmentEmbeddable() {
        // for JPA
    }

    public ShipmentEmbeddable(String carrier, String trackingNumber,
                              LocalDate shippedOn, LocalDate estimatedDelivery) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.shippedOn = shippedOn;
        this.estimatedDelivery = estimatedDelivery;
    }

    /** Null when the order has not shipped, which is why the carrier is the test. */
    public Shipment toDomain() {
        return carrier == null ? null
                : new Shipment(carrier, trackingNumber, shippedOn, estimatedDelivery);
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDate estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public String getCarrier() {
        return carrier;
    }
}
