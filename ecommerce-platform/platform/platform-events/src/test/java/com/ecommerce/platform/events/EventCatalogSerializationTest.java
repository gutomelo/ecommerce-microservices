package com.ecommerce.platform.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip de (de)serializacao JSON para cada evento do catalogo
 * (docs/events/catalogo-eventos.md), garantindo que os payloads (incluindo
 * records aninhados) sobrevivem intactos e que eventType/aggregateType sao
 * preenchidos corretamente pela factory `of(...)`.
 */
class EventCatalogSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String CORRELATION_ID = "corr-123";
    private static final String TRACE_ID = "trace-abc";
    private static final String ORDER_ID = UUID.randomUUID().toString();

    @Test
    void orderCreatedEventRoundTrip() throws Exception {
        var payload = new OrderCreatedEvent.Payload(
                UUID.fromString(ORDER_ID),
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 2, new BigDecimal("19.90"))),
                new BigDecimal("39.80"));

        OrderCreatedEvent original = OrderCreatedEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("OrderCreated");
        assertThat(original.getAggregateType()).isEqualTo("Order");
        assertThat(original.getAggregateId()).isEqualTo(ORDER_ID);

        String json = MAPPER.writeValueAsString(original);
        OrderCreatedEvent parsed = MAPPER.readValue(json, OrderCreatedEvent.class);

        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getPayload().items()).hasSize(1);
    }

    @Test
    void stockReservedEventRoundTrip() throws Exception {
        var payload = new StockReservedEvent.Payload(
                UUID.fromString(ORDER_ID),
                List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 2)));

        StockReservedEvent original = StockReservedEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("StockReserved");
        assertThat(original.getAggregateType()).isEqualTo("Stock");

        String json = MAPPER.writeValueAsString(original);
        StockReservedEvent parsed = MAPPER.readValue(json, StockReservedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void stockUnavailableEventRoundTrip() throws Exception {
        var payload = new StockUnavailableEvent.Payload(
                UUID.fromString(ORDER_ID), List.of(UUID.randomUUID()), "estoque insuficiente");

        StockUnavailableEvent original = StockUnavailableEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("StockUnavailable");
        assertThat(original.getAggregateType()).isEqualTo("Stock");

        String json = MAPPER.writeValueAsString(original);
        StockUnavailableEvent parsed = MAPPER.readValue(json, StockUnavailableEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void paymentApprovedEventRoundTrip() throws Exception {
        var payload = new PaymentApprovedEvent.Payload(
                UUID.fromString(ORDER_ID), UUID.randomUUID(), new BigDecimal("39.80"), Instant.parse("2026-07-28T12:00:00Z"));

        PaymentApprovedEvent original = PaymentApprovedEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("PaymentApproved");
        assertThat(original.getAggregateType()).isEqualTo("Payment");

        String json = MAPPER.writeValueAsString(original);
        PaymentApprovedEvent parsed = MAPPER.readValue(json, PaymentApprovedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void paymentDeclinedEventRoundTrip() throws Exception {
        var payload = new PaymentDeclinedEvent.Payload(UUID.fromString(ORDER_ID), new BigDecimal("39.80"), "saldo insuficiente");

        PaymentDeclinedEvent original = PaymentDeclinedEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("PaymentDeclined");
        assertThat(original.getAggregateType()).isEqualTo("Payment");

        String json = MAPPER.writeValueAsString(original);
        PaymentDeclinedEvent parsed = MAPPER.readValue(json, PaymentDeclinedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void orderConfirmedEventRoundTrip() throws Exception {
        var payload = new OrderConfirmedEvent.Payload(
                UUID.fromString(ORDER_ID), UUID.randomUUID(), Instant.parse("2026-07-28T12:05:00Z"));

        OrderConfirmedEvent original = OrderConfirmedEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("OrderConfirmed");
        assertThat(original.getAggregateType()).isEqualTo("Order");

        String json = MAPPER.writeValueAsString(original);
        OrderConfirmedEvent parsed = MAPPER.readValue(json, OrderConfirmedEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void orderCancelledEventRoundTrip() throws Exception {
        var payload = new OrderCancelledEvent.Payload(
                UUID.fromString(ORDER_ID), UUID.randomUUID(), "pagamento recusado", Instant.parse("2026-07-28T12:05:00Z"));

        OrderCancelledEvent original = OrderCancelledEvent.of(ORDER_ID, CORRELATION_ID, TRACE_ID, payload);

        assertThat(original.getEventType()).isEqualTo("OrderCancelled");
        assertThat(original.getAggregateType()).isEqualTo("Order");

        String json = MAPPER.writeValueAsString(original);
        OrderCancelledEvent parsed = MAPPER.readValue(json, OrderCancelledEvent.class);

        assertThat(parsed).isEqualTo(original);
    }
}
