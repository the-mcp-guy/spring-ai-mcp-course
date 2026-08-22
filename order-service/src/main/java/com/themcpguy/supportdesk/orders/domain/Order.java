package com.themcpguy.supportdesk.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * An order, as the rest of the application sees one.
 *
 * <p>This is the type the REST controller returns and, from Class 2, the type the MCP
 * tools return. The JPA entities behind it are an implementation detail of the
 * repository layer.
 *
 * <p>{@code shipment} is null until an order ships. Marking it {@link Nullable} keeps it
 * out of the {@code required} list in the output schema Class 3 generates, and
 * {@code NON_NULL} leaves the field out of the JSON instead of sending null, because a
 * schema that declares an object and a payload that sends null fail validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Order(
        String orderId,
        OrderStatus status,
        Customer customer,
        List<OrderItem> items,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant lastUpdated,
        @Nullable Shipment shipment) {
}
