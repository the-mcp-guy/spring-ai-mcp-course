package com.themcpguy.supportdesk.orders.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.themcpguy.supportdesk.orders.entity.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
}
