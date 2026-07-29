package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reune a logica repetitiva que todo listener SQS precisaria implementar:
 * desserializar o corpo da mensagem, verificar idempotencia e delegar para o
 * {@link EventConsumer} da aplicacao. Cada servico chama {@link #dispatch(String)}
 * de dentro do seu proprio metodo anotado com {@code @SqsListener} (a anotacao em
 * si nao e abstraivel, mas toda a logica ao redor dela sim).
 */
public class IdempotentEventDispatcher<T extends BaseEvent<?>> {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventDispatcher.class);

    private final MessageDeserializer deserializer;
    private final ProcessedEventChecker processedEventChecker;
    private final EventConsumer<T> consumer;
    private final Class<T> eventType;

    public IdempotentEventDispatcher(MessageDeserializer deserializer,
                                      ProcessedEventChecker processedEventChecker,
                                      EventConsumer<T> consumer,
                                      Class<T> eventType) {
        this.deserializer = deserializer;
        this.processedEventChecker = processedEventChecker;
        this.consumer = consumer;
        this.eventType = eventType;
    }

    public void dispatch(String rawMessageBody) {
        T event = deserializer.deserialize(rawMessageBody, eventType);

        if (processedEventChecker.isProcessed(event.getEventId())) {
            log.info("Evento {} (eventId={}) ja processado, ignorando (idempotencia)",
                    event.getEventType(), event.getEventId());
            return;
        }

        consumer.consume(event);
        processedEventChecker.markProcessed(event.getEventId());
    }
}
