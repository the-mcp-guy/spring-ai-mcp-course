package com.themcpguy.supportdesk.orders.domain;

import java.util.Set;

/**
 * The lifecycle of an order.
 *
 * <p>Only PENDING and PROCESSING orders can be cancelled: once a parcel is with a
 * carrier, cancelling it is a returns problem rather than an order problem.
 */
public enum OrderStatus {

    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Set<OrderStatus> CANCELLABLE = Set.of(PENDING, PROCESSING);

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }

    /**
     * Parse a status supplied as text, with a message naming the valid values when it
     * does not match. The message is read by a model from Class 3 onwards, so it has to
     * say what would work instead.
     */
    public static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "A status is required. Must be one of: " + java.util.Arrays.toString(values()));
        }
        try {
            return valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status '%s'. Must be one of: %s"
                            .formatted(value, java.util.Arrays.toString(values())));
        }
    }
}
