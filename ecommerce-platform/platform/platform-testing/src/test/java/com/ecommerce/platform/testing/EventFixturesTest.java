package com.ecommerce.platform.testing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventFixturesTest {

    @Test
    void orderCreatedEventIsSelfConsistent() {
        var event = EventFixtures.orderCreatedEvent("corr-1");

        assertThat(event.getCorrelationId()).isEqualTo("corr-1");
        assertThat(event.getAggregateId()).isEqualTo(event.getPayload().orderId().toString());
        assertThat(event.getEventType()).isEqualTo("OrderCreated");
        assertThat(event.getPayload().items()).isNotEmpty();
    }

    @Test
    void orderCreatedEventWithoutCorrelationIdGeneratesOne() {
        var event = EventFixtures.orderCreatedEvent();

        assertThat(event.getCorrelationId()).isNotBlank();
    }

    @Test
    void stockReservedEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.stockReservedEvent(orderId, "corr-2");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("StockReserved");
    }

    @Test
    void stockUnavailableEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.stockUnavailableEvent(orderId, "corr-3");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("StockUnavailable");
        assertThat(event.getPayload().reason()).isNotBlank();
    }

    @Test
    void paymentApprovedEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.paymentApprovedEvent(orderId, "corr-4");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("PaymentApproved");
    }

    @Test
    void paymentDeclinedEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.paymentDeclinedEvent(orderId, "corr-5");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("PaymentDeclined");
    }

    @Test
    void orderConfirmedEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.orderConfirmedEvent(orderId, "corr-6");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("OrderConfirmed");
    }

    @Test
    void orderCancelledEventReferencesGivenOrder() {
        String orderId = java.util.UUID.randomUUID().toString();
        var event = EventFixtures.orderCancelledEvent(orderId, "corr-7");

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getEventType()).isEqualTo("OrderCancelled");
        assertThat(event.getPayload().reason()).isNotBlank();
    }
}
