package com.ecommerce.paymentservice.application.consumer;

import com.ecommerce.paymentservice.application.PaymentService;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class StockReservedConsumer implements EventConsumer<StockReservedEvent> {

    private final PaymentService paymentService;

    public StockReservedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void consume(StockReservedEvent event) {
        var payload = event.getPayload();
        paymentService.processPayment(payload.orderId(), payload.totalAmount(), event.getCorrelationId(), event.getTraceId());
    }
}
