package com.themcpguy.supportdesk.orders.domain;

import java.math.BigDecimal;

/**
 * One line of an order.
 */
public record OrderItem(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
