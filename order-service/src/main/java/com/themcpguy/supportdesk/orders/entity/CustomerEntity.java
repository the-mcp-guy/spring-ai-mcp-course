package com.themcpguy.supportdesk.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.themcpguy.supportdesk.orders.domain.Customer;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    private String name;

    private String email;

    private String tier;

    protected CustomerEntity() {
        // for JPA
    }

    public CustomerEntity(String customerId, String name, String email, String tier) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.tier = tier;
    }

    public Customer toDomain() {
        return new Customer(customerId, name, email, tier);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTier() {
        return tier;
    }
}
