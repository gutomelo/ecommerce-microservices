package com.ecommerce.inventoryservice.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Registro imutavel de uma reserva de estoque bem-sucedida para um pedido,
 * usado para saber exatamente o que liberar na compensacao (OrderCancelledEvent).
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
public class StockReservation {

    private final UUID id;
    private final UUID orderId;
    private final List<ReservedItem> items;
    private final Instant createdAt;

    public static StockReservation create(UUID orderId, List<ReservedItem> items) {
        return StockReservation.builder().orderId(orderId).items(items).build();
    }

    public record ReservedItem(UUID productId, int quantity) {
    }
}
