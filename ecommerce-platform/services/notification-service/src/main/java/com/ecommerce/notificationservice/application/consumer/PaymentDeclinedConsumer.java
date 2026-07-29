package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentDeclinedConsumer implements EventConsumer<PaymentDeclinedEvent> {

    private final NotificationService notificationService;

    public PaymentDeclinedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(PaymentDeclinedEvent event) {
        notificationService.logIntermediateSagaEvent(PaymentDeclinedEvent.EVENT_TYPE, event.getAggregateId());
    }
}
