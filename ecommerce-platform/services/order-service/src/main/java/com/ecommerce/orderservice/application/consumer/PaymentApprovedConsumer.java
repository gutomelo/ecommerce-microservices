package com.ecommerce.orderservice.application.consumer;

import com.ecommerce.orderservice.application.OrderService;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentApprovedConsumer implements EventConsumer<PaymentApprovedEvent> {

    private final OrderService orderService;

    public PaymentApprovedConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void consume(PaymentApprovedEvent event) {
        orderService.confirmOrder(event.getPayload().orderId(), event.getCorrelationId(), event.getTraceId());
    }
}
