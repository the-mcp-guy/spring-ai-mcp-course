package com.themcpguy.supportdesk.orders.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.themcpguy.supportdesk.orders.domain.OrderStatus;
import com.themcpguy.supportdesk.orders.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByCustomerCustomerIdOrderByCreatedAtDesc(String customerId);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Backs the order-ID completion in Class 5. */
    List<OrderEntity> findByOrderIdStartingWithOrderByOrderIdAsc(String prefix, Limit limit);
}
