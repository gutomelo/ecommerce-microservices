package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer implements EventConsumer<OrderCreatedEvent> {

    private final NotificationService notificationService;

    public OrderCreatedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(OrderCreatedEvent event) {
        notificationService.notifyOrderCreated(event.getPayload().orderId(), event.getPayload().customerId());
    }
}
