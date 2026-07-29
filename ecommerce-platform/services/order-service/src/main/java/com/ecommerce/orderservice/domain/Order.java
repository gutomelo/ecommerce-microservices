package com.ecommerce.orderservice.domain;

import com.ecommerce.platform.exception.ValidationException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root do order-service - o nucleo da Saga (ver CLAUDE.md). Sem
 * anotacoes de framework; a persistencia e mapeada em infrastructure/persistence
 * (ver .claude/rules/estrutura-microsservico.md). Nunca chama outro servico
 * diretamente: toda mudanca de estado pos-criacao vem de eventos consumidos
 * (PaymentApproved/PaymentDeclined/StockUnavailable).
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Order {

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Order create(UUID customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("O pedido precisa ter ao menos um item");
        }
        BigDecimal totalAmount = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Order.builder()
                .customerId(customerId)
                .items(items)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();
    }

    public Order confirm() {
        requirePending("confirmar");
        return this.toBuilder().status(OrderStatus.CONFIRMED).build();
    }

    public Order cancel() {
        requirePending("cancelar");
        return this.toBuilder().status(OrderStatus.CANCELLED).build();
    }

    private void requirePending(String action) {
        if (status != OrderStatus.PENDING) {
            throw new ValidationException(
                    "Nao e possivel %s um pedido no status %s".formatted(action, status));
        }
    }
}
