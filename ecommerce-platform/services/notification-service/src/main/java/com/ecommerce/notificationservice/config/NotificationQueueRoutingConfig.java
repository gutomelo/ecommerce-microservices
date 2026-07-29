package com.ecommerce.notificationservice.config;

import com.ecommerce.notificationservice.application.consumer.OrderCancelledConsumer;
import com.ecommerce.notificationservice.application.consumer.OrderConfirmedConsumer;
import com.ecommerce.notificationservice.application.consumer.OrderCreatedConsumer;
import com.ecommerce.notificationservice.application.consumer.PaymentApprovedConsumer;
import com.ecommerce.notificationservice.application.consumer.PaymentDeclinedConsumer;
import com.ecommerce.notificationservice.application.consumer.StockReservedConsumer;
import com.ecommerce.notificationservice.application.consumer.StockUnavailableConsumer;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.EventTypeRouter;
import com.ecommerce.platform.messaging.IdempotentEventDispatcher;
import com.ecommerce.platform.messaging.MessageDeserializer;
import com.ecommerce.platform.messaging.ProcessedEventChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as rotas do EventTypeRouter da fila notification-queue, uma para cada
 * um dos 7 eventos do catalogo. Sobrepoe o bean default de
 * MessagingAutoConfiguration (@ConditionalOnMissingBean) porque so o
 * notification-service sabe quais EventConsumer tratam quais eventType.
 */
@Configuration
public class NotificationQueueRoutingConfig {

    @Bean
    public EventTypeRouter eventTypeRouter(ObjectMapper objectMapper,
                                            MessageDeserializer deserializer,
                                            ProcessedEventChecker processedEventChecker,
                                            OrderCreatedConsumer orderCreatedConsumer,
                                            StockReservedConsumer stockReservedConsumer,
                                            StockUnavailableConsumer stockUnavailableConsumer,
                                            PaymentApprovedConsumer paymentApprovedConsumer,
                                            PaymentDeclinedConsumer paymentDeclinedConsumer,
                                            OrderConfirmedConsumer orderConfirmedConsumer,
                                            OrderCancelledConsumer orderCancelledConsumer) {
        EventTypeRouter router = new EventTypeRouter(objectMapper);

        router.register(OrderCreatedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, orderCreatedConsumer, OrderCreatedEvent.class)::dispatch);
        router.register(StockReservedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, stockReservedConsumer, StockReservedEvent.class)::dispatch);
        router.register(StockUnavailableEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, stockUnavailableConsumer, StockUnavailableEvent.class)::dispatch);
        router.register(PaymentApprovedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, paymentApprovedConsumer, PaymentApprovedEvent.class)::dispatch);
        router.register(PaymentDeclinedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, paymentDeclinedConsumer, PaymentDeclinedEvent.class)::dispatch);
        router.register(OrderConfirmedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, orderConfirmedConsumer, OrderConfirmedEvent.class)::dispatch);
        router.register(OrderCancelledEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, orderCancelledConsumer, OrderCancelledEvent.class)::dispatch);

        return router;
    }
}
