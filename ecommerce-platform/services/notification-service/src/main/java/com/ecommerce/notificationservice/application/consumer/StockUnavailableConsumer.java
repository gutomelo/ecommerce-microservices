package com.ecommerce.notificationservice.application.consumer;

import com.ecommerce.notificationservice.application.NotificationService;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class StockUnavailableConsumer implements EventConsumer<StockUnavailableEvent> {

    private final NotificationService notificationService;

    public StockUnavailableConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void consume(StockUnavailableEvent event) {
        notificationService.logIntermediateSagaEvent(StockUnavailableEvent.EVENT_TYPE, event.getAggregateId());
    }
}
