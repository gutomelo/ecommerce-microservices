package com.ecommerce.orderservice.application.consumer;

import com.ecommerce.orderservice.application.OrderService;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentDeclinedConsumer implements EventConsumer<PaymentDeclinedEvent> {

    private final OrderService orderService;

    public PaymentDeclinedConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void consume(PaymentDeclinedEvent event) {
        orderService.cancelOrder(event.getPayload().orderId(), event.getPayload().reason(),
                event.getCorrelationId(), event.getTraceId());
    }
}
