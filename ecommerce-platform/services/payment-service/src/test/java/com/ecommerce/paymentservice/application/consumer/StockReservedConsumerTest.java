package com.ecommerce.paymentservice.application.consumer;

import com.ecommerce.paymentservice.application.PaymentService;
import com.ecommerce.platform.events.StockReservedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StockReservedConsumerTest {

    @Test
    void delegatesToProcessPaymentWithOrderIdAndTotalAmount() {
        PaymentService paymentService = mock(PaymentService.class);
        StockReservedConsumer consumer = new StockReservedConsumer(paymentService);

        UUID orderId = UUID.randomUUID();
        var payload = new StockReservedEvent.Payload(orderId,
                List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 2)),
                new BigDecimal("39.80"));
        var event = StockReservedEvent.of(orderId.toString(), "corr-1", "trace-1", payload);

        consumer.consume(event);

        verify(paymentService).processPayment(orderId, new BigDecimal("39.80"), "corr-1", "trace-1");
    }
}
