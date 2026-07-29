package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class StockReservedConsumer implements EventConsumer<StockReservedEvent> {

    private final NotificationService notificationService;

    public StockReservedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(StockReservedEvent event) {
        notificationService.logIntermediateSagaEvent(StockReservedEvent.EVENT_TYPE, event.getAggregateId());
    }
}
