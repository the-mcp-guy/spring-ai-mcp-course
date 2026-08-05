package com.themcpguy.supportdesk.orders.domain;

import java.time.LocalDate;

/**
 * Where a parcel is, once an order has shipped.
 *
 * <p>Null on an order that has not shipped yet. Class 11 refreshes the estimate across
 * every shipment still in transit, which is the job worth reporting progress on.
 */
public record Shipment(
        String carrier,
        String trackingNumber,
        LocalDate shippedOn,
        LocalDate estimatedDelivery) {
}
