package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class OrderConfirmedConsumer implements EventConsumer<OrderConfirmedEvent> {

    private final NotificationService notificationService;

    public OrderConfirmedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(OrderConfirmedEvent event) {
        notificationService.notifyOrderConfirmed(event.getPayload().orderId(), event.getPayload().customerId());
    }
}
