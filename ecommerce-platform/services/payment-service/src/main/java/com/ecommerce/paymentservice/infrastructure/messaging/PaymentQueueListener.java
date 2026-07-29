package com.ecommerce.paymentservice.infrastructure.messaging;

import com.ecommerce.paymentservice.application.consumer.StockReservedConsumer;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.messaging.IdempotentEventDispatcher;
import com.ecommerce.platform.messaging.MessageDeserializer;
import com.ecommerce.platform.messaging.ProcessedEventChecker;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

/**
 * Unico ponto de entrada da fila payment-queue. Diferente de order-queue/
 * inventory-queue, payment-queue recebe apenas um eventType (StockReserved -
 * ver infrastructure/localstack/init/init-aws.sh), entao nao precisa do
 * EventTypeRouter: o IdempotentEventDispatcher e usado diretamente.
 */
@Component
public class PaymentQueueListener {

    private final IdempotentEventDispatcher<StockReservedEvent> dispatcher;

    public PaymentQueueListener(MessageDeserializer deserializer, ProcessedEventChecker processedEventChecker,
                                 StockReservedConsumer stockReservedConsumer) {
        this.dispatcher = new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, stockReservedConsumer, StockReservedEvent.class);
    }

    @SqsListener("payment-queue")
    public void listen(String rawMessageBody) {
        dispatcher.dispatch(rawMessageBody);
    }
}
