package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationQueueConsumersTest {

    @Test
    void orderCreatedConsumerNotifiesOrderCreated() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderCreatedEvent.Payload(orderId, customerId,
                List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 1, BigDecimal.TEN)), BigDecimal.TEN);
        var event = OrderCreatedEvent.of(orderId.toString(), "c", "t", payload);

        new OrderCreatedConsumer(service).consume(event);

        verify(service).notifyOrderCreated(orderId, customerId);
    }

    @Test
    void orderConfirmedConsumerNotifiesOrderConfirmed() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderConfirmedEvent.Payload(orderId, customerId, Instant.now());
        var event = OrderConfirmedEvent.of(orderId.toString(), "c", "t", payload);

        new OrderConfirmedConsumer(service).consume(event);

        verify(service).notifyOrderConfirmed(orderId, customerId);
    }

    @Test
    void orderCancelledConsumerNotifiesOrderCancelled() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderCancelledEvent.Payload(orderId, customerId, "pagamento recusado", Instant.now());
        var event = OrderCancelledEvent.of(orderId.toString(), "c", "t", payload);

        new OrderCancelledConsumer(service).consume(event);

        verify(service).notifyOrderCancelled(orderId, customerId, "pagamento recusado");
    }

    @Test
    void stockReservedConsumerLogsIntermediateEvent() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        var payload = new StockReservedEvent.Payload(orderId,
                List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 1)), BigDecimal.TEN);
        var event = StockReservedEvent.of(orderId.toString(), "c", "t", payload);

        new StockReservedConsumer(service).consume(event);

        verify(service).logIntermediateSagaEvent(StockReservedEvent.EVENT_TYPE, orderId.toString());
    }

    @Test
    void stockUnavailableConsumerLogsIntermediateEvent() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        var payload = new StockUnavailableEvent.Payload(orderId, List.of(UUID.randomUUID()), "sem estoque");
        var event = StockUnavailableEvent.of(orderId.toString(), "c", "t", payload);

        new StockUnavailableConsumer(service).consume(event);

        verify(service).logIntermediateSagaEvent(StockUnavailableEvent.EVENT_TYPE, orderId.toString());
    }

    @Test
    void paymentApprovedConsumerLogsIntermediateEvent() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        var payload = new PaymentApprovedEvent.Payload(orderId, UUID.randomUUID(), BigDecimal.TEN, Instant.now());
        var event = PaymentApprovedEvent.of(orderId.toString(), "c", "t", payload);

        new PaymentApprovedConsumer(service).consume(event);

        verify(service).logIntermediateSagaEvent(PaymentApprovedEvent.EVENT_TYPE, orderId.toString());
    }

    @Test
    void paymentDeclinedConsumerLogsIntermediateEvent() {
        NotificationService service = mock(NotificationService.class);
        UUID orderId = UUID.randomUUID();
        var payload = new PaymentDeclinedEvent.Payload(orderId, BigDecimal.TEN, "saldo insuficiente");
        var event = PaymentDeclinedEvent.of(orderId.toString(), "c", "t", payload);

        new PaymentDeclinedConsumer(service).consume(event);

        verify(service).logIntermediateSagaEvent(PaymentDeclinedEvent.EVENT_TYPE, orderId.toString());
    }
}
