package com.ecommerce.inventoryservice.config;

import com.ecommerce.inventoryservice.application.consumer.OrderCancelledConsumer;
import com.ecommerce.inventoryservice.application.consumer.OrderCreatedConsumer;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.messaging.EventTypeRouter;
import com.ecommerce.platform.messaging.IdempotentEventDispatcher;
import com.ecommerce.platform.messaging.MessageDeserializer;
import com.ecommerce.platform.messaging.ProcessedEventChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as rotas do EventTypeRouter da fila inventory-queue. Sobrepoe o bean
 * default de MessagingAutoConfiguration (@ConditionalOnMissingBean) porque so o
 * inventory-service sabe quais EventConsumer tratam quais eventType.
 */
@Configuration
public class InventoryQueueRoutingConfig {

    @Bean
    public EventTypeRouter eventTypeRouter(ObjectMapper objectMapper,
                                            MessageDeserializer deserializer,
                                            ProcessedEventChecker processedEventChecker,
                                            OrderCreatedConsumer orderCreatedConsumer,
                                            OrderCancelledConsumer orderCancelledConsumer) {
        EventTypeRouter router = new EventTypeRouter(objectMapper);

        router.register(OrderCreatedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, orderCreatedConsumer, OrderCreatedEvent.class)::dispatch);
        router.register(OrderCancelledEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, orderCancelledConsumer, OrderCancelledEvent.class)::dispatch);

        return router;
    }
}
