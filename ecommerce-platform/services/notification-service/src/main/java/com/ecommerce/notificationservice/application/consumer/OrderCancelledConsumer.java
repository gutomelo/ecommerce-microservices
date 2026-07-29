package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelledConsumer implements EventConsumer<OrderCancelledEvent> {

    private final NotificationService notificationService;

    public OrderCancelledConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(OrderCancelledEvent event) {
        notificationService.notifyOrderCancelled(
                event.getPayload().orderId(), event.getPayload().customerId(), event.getPayload().reason());
    }
}
