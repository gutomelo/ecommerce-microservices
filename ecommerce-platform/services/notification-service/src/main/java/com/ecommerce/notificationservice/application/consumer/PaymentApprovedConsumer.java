package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentApprovedConsumer implements EventConsumer<PaymentApprovedEvent> {

    private final NotificationService notificationService;

    public PaymentApprovedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(PaymentApprovedEvent event) {
        notificationService.logIntermediateSagaEvent(PaymentApprovedEvent.EVENT_TYPE, event.getAggregateId());
    }
}
