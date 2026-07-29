package com.ecommerce.inventoryservice.application.consumer;

import com.ecommerce.inventoryservice.application.InventoryService;
import com.ecommerce.inventoryservice.application.RequestedItem;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.messaging.EventConsumer;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer implements EventConsumer<OrderCreatedEvent> {

    private final InventoryService inventoryService;

    public OrderCreatedConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void consume(OrderCreatedEvent event) {
        var payload = event.getPayload();
        var requestedItems = payload.items().stream()
                .map(item -> new RequestedItem(item.productId(), item.quantity()))
                .toList();
        inventoryService.reserveStock(payload.orderId(), requestedItems, payload.totalAmount(),
                event.getCorrelationId(), event.getTraceId());
    }
}
