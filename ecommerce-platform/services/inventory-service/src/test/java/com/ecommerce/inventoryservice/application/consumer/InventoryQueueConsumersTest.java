package com.ecommerce.inventoryservice.application.consumer;

import com.ecommerce.inventoryservice.application.InventoryService;
import com.ecommerce.inventoryservice.application.RequestedItem;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InventoryQueueConsumersTest {

    @Test
    void orderCreatedConsumerDelegatesToReserveStock() {
        InventoryService inventoryService = mock(InventoryService.class);
        OrderCreatedConsumer consumer = new OrderCreatedConsumer(inventoryService);

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var payload = new OrderCreatedEvent.Payload(orderId, UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Payload.Item(productId, 2, new BigDecimal("19.90"))),
                new BigDecimal("39.80"));
        var event = OrderCreatedEvent.of(orderId.toString(), "corr-1", "trace-1", payload);

        consumer.consume(event);

        verify(inventoryService).reserveStock(eq(orderId), eq(List.of(new RequestedItem(productId, 2))),
                eq(new BigDecimal("39.80")), eq("corr-1"), eq("trace-1"));
    }

    @Test
    void orderCancelledConsumerDelegatesToReleaseStock() {
        InventoryService inventoryService = mock(InventoryService.class);
        OrderCancelledConsumer consumer = new OrderCancelledConsumer(inventoryService);

        UUID orderId = UUID.randomUUID();
        var payload = new OrderCancelledEvent.Payload(orderId, UUID.randomUUID(), "pagamento recusado", Instant.now());
        var event = OrderCancelledEvent.of(orderId.toString(), "corr-2", "trace-2", payload);

        consumer.consume(event);

        verify(inventoryService).releaseStock(orderId);
    }
}
