package com.ecommerce.inventoryservice.application.consumer;

import com.ecommerce.inventoryservice.application.InventoryService;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelledConsumer implements EventConsumer<OrderCancelledEvent> {

    private final InventoryService inventoryService;

    public OrderCancelledConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void consume(OrderCancelledEvent event) {
        inventoryService.releaseStock(event.getPayload().orderId());
    }
}
