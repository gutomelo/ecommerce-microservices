package com.ecommerce.orderservice.api.dto;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, UUID customerId, List<ItemResponse> items, BigDecimal totalAmount,
                             OrderStatus status, Instant createdAt, Instant updatedAt) {

    public static OrderResponse from(Order order) {
        List<ItemResponse> items = order.getItems().stream().map(ItemResponse::from).toList();
        return new OrderResponse(order.getId(), order.getCustomerId(), items, order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt(), order.getUpdatedAt());
    }

    public record ItemResponse(UUID productId, int quantity, BigDecimal unitPrice) {

        public static ItemResponse from(OrderItem item) {
            return new ItemResponse(item.productId(), item.quantity(), item.unitPrice());
        }
    }
}
