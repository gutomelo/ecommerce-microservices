package com.ecommerce.orderservice.application.consumer;

import com.ecommerce.orderservice.application.OrderService;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderQueueConsumersTest {

    @Test
    void paymentApprovedConsumerDelegatesToConfirmOrder() {
        OrderService orderService = mock(OrderService.class);
        PaymentApprovedConsumer consumer = new PaymentApprovedConsumer(orderService);

        UUID orderId = UUID.randomUUID();
        var payload = new PaymentApprovedEvent.Payload(orderId, UUID.randomUUID(), new BigDecimal("39.80"), Instant.now());
        var event = PaymentApprovedEvent.of(orderId.toString(), "corr-1", "trace-1", payload);

        consumer.consume(event);

        verify(orderService).confirmOrder(orderId, "corr-1", "trace-1");
    }

    @Test
    void paymentDeclinedConsumerDelegatesToCancelOrder() {
        OrderService orderService = mock(OrderService.class);
        PaymentDeclinedConsumer consumer = new PaymentDeclinedConsumer(orderService);

        UUID orderId = UUID.randomUUID();
        var payload = new PaymentDeclinedEvent.Payload(orderId, new BigDecimal("39.80"), "saldo insuficiente");
        var event = PaymentDeclinedEvent.of(orderId.toString(), "corr-2", "trace-2", payload);

        consumer.consume(event);

        verify(orderService).cancelOrder(orderId, "saldo insuficiente", "corr-2", "trace-2");
    }

    @Test
    void stockUnavailableConsumerDelegatesToCancelOrder() {
        OrderService orderService = mock(OrderService.class);
        StockUnavailableConsumer consumer = new StockUnavailableConsumer(orderService);

        UUID orderId = UUID.randomUUID();
        var payload = new StockUnavailableEvent.Payload(orderId, List.of(UUID.randomUUID()), "estoque insuficiente");
        var event = StockUnavailableEvent.of(orderId.toString(), "corr-3", "trace-3", payload);

        consumer.consume(event);

        verify(orderService).cancelOrder(orderId, "estoque insuficiente", "corr-3", "trace-3");
    }
}
