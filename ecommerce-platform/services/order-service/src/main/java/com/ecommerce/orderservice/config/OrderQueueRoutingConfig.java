package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.application.consumer.PaymentApprovedConsumer;
import com.ecommerce.orderservice.application.consumer.PaymentDeclinedConsumer;
import com.ecommerce.orderservice.application.consumer.StockUnavailableConsumer;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.EventTypeRouter;
import com.ecommerce.platform.messaging.IdempotentEventDispatcher;
import com.ecommerce.platform.messaging.MessageDeserializer;
import com.ecommerce.platform.messaging.ProcessedEventChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as rotas do EventTypeRouter da fila order-queue. Sobrepoe o bean
 * default de MessagingAutoConfiguration (@ConditionalOnMissingBean) porque so o
 * order-service sabe quais EventConsumer tratam quais eventType.
 */
@Configuration
public class OrderQueueRoutingConfig {

    @Bean
    public EventTypeRouter eventTypeRouter(ObjectMapper objectMapper,
                                            MessageDeserializer deserializer,
                                            ProcessedEventChecker processedEventChecker,
                                            PaymentApprovedConsumer paymentApprovedConsumer,
                                            PaymentDeclinedConsumer paymentDeclinedConsumer,
                                            StockUnavailableConsumer stockUnavailableConsumer) {
        EventTypeRouter router = new EventTypeRouter(objectMapper);

        router.register(PaymentApprovedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, paymentApprovedConsumer, PaymentApprovedEvent.class)::dispatch);
        router.register(PaymentDeclinedEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, paymentDeclinedConsumer, PaymentDeclinedEvent.class)::dispatch);
        router.register(StockUnavailableEvent.EVENT_TYPE, new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, stockUnavailableConsumer, StockUnavailableEvent.class)::dispatch);

        return router;
    }
}
