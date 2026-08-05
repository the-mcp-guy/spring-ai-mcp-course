package com.themcpguy.supportdesk.orders.domain;

/**
 * A customer, as the rest of the application sees one.
 *
 * <p>The name and email are here because Class 5 drafts a refund email and cannot do
 * that from a customer ID alone.
 */
public record Customer(
        String customerId,
        String name,
        String email,
        String tier) {
}
