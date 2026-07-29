package com.ecommerce.orderservice.application.consumer;

import com.ecommerce.orderservice.application.OrderService;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class StockUnavailableConsumer implements EventConsumer<StockUnavailableEvent> {

    private final OrderService orderService;

    public StockUnavailableConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void consume(StockUnavailableEvent event) {
        orderService.cancelOrder(event.getPayload().orderId(), event.getPayload().reason(),
                event.getCorrelationId(), event.getTraceId());
    }
}
