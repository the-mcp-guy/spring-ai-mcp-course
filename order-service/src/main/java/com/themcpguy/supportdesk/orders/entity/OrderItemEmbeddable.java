package com.themcpguy.supportdesk.orders.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.themcpguy.supportdesk.orders.domain.OrderItem;

@Embeddable
public class OrderItemEmbeddable {

    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    private int quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItemEmbeddable() {
        // for JPA
    }

    public OrderItemEmbeddable(String productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public OrderItem toDomain() {
        return new OrderItem(productId, productName, quantity, unitPrice);
    }
}
