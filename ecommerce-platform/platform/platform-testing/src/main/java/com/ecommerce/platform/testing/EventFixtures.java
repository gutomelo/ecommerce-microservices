package com.ecommerce.platform.testing;

import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Factories dos eventos do catalogo (docs/events/catalogo-eventos.md) com valores
 * validos e randomizados, para reduzir boilerplate nos testes de cada
 * microsservico. Nunca duplique a definicao dos eventos aqui - so monta instancias
 * usando as classes de platform-events.
 */
public final class EventFixtures {

    private EventFixtures() {
    }

    public static OrderCreatedEvent orderCreatedEvent() {
        return orderCreatedEvent(UUID.randomUUID().toString());
    }

    public static OrderCreatedEvent orderCreatedEvent(String correlationId) {
        UUID orderId = UUID.randomUUID();
        var payload = new OrderCreatedEvent.Payload(
                orderId,
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 2, new BigDecimal("19.90"))),
                new BigDecimal("39.80"));
        return OrderCreatedEvent.of(orderId.toString(), correlationId, UUID.randomUUID().toString(), payload);
    }

    public static StockReservedEvent stockReservedEvent(String orderId, String correlationId) {
        var payload = new StockReservedEvent.Payload(
                UUID.fromString(orderId),
                List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 2)));
        return StockReservedEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }

    public static StockUnavailableEvent stockUnavailableEvent(String orderId, String correlationId) {
        var payload = new StockUnavailableEvent.Payload(
                UUID.fromString(orderId), List.of(UUID.randomUUID()), "estoque insuficiente");
        return StockUnavailableEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }

    public static PaymentApprovedEvent paymentApprovedEvent(String orderId, String correlationId) {
        var payload = new PaymentApprovedEvent.Payload(
                UUID.fromString(orderId), UUID.randomUUID(), new BigDecimal("39.80"), Instant.now());
        return PaymentApprovedEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }

    public static PaymentDeclinedEvent paymentDeclinedEvent(String orderId, String correlationId) {
        var payload = new PaymentDeclinedEvent.Payload(UUID.fromString(orderId), new BigDecimal("39.80"), "saldo insuficiente");
        return PaymentDeclinedEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }

    public static OrderConfirmedEvent orderConfirmedEvent(String orderId, String correlationId) {
        var payload = new OrderConfirmedEvent.Payload(UUID.fromString(orderId), UUID.randomUUID(), Instant.now());
        return OrderConfirmedEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }

    public static OrderCancelledEvent orderCancelledEvent(String orderId, String correlationId) {
        var payload = new OrderCancelledEvent.Payload(
                UUID.fromString(orderId), UUID.randomUUID(), "pagamento recusado", Instant.now());
        return OrderCancelledEvent.of(orderId, correlationId, UUID.randomUUID().toString(), payload);
    }
}
