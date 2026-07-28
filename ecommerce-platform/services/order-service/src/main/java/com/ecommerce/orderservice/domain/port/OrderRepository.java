package com.ecommerce.orderservice.domain.port;

import com.ecommerce.orderservice.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);
}
