package com.themcpguy.supportdesk.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An order, as the rest of the application sees one.
 *
 * <p>This is the type the REST controller returns and, from Class 2, the type the MCP
 * tools return. The JPA entities behind it are an implementation detail of the
 * repository layer.
 */
public record Order(
        String orderId,
        OrderStatus status,
        Customer customer,
        List<OrderItem> items,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant lastUpdated,
        Shipment shipment) {
}
