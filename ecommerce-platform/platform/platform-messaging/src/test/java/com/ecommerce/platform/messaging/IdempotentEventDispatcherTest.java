package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotentEventDispatcherTest {

    @Getter
    @SuperBuilder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SampleEvent extends BaseEvent<String> {
    }

    private MessageDeserializer deserializer;
    private ProcessedEventChecker processedEventChecker;
    private EventConsumer<SampleEvent> consumer;
    private IdempotentEventDispatcher<SampleEvent> dispatcher;
    private SampleEvent sampleEvent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        deserializer = mock(MessageDeserializer.class);
        processedEventChecker = mock(ProcessedEventChecker.class);
        consumer = mock(EventConsumer.class);
        dispatcher = new IdempotentEventDispatcher<>(deserializer, processedEventChecker, consumer, SampleEvent.class);

        sampleEvent = SampleEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateId("order-1")
                .aggregateType("Order")
                .eventType("Sample")
                .occurredAt(Instant.now())
                .correlationId("corr-1")
                .traceId("trace-1")
                .version(1)
                .payload("hello")
                .build();

        when(deserializer.deserialize("raw-json", SampleEvent.class)).thenReturn(sampleEvent);
    }

    @Test
    void dispatchesAndMarksProcessedWhenNotYetProcessed() {
        when(processedEventChecker.isProcessed(sampleEvent.getEventId())).thenReturn(false);

        dispatcher.dispatch("raw-json");

        verify(consumer).consume(sampleEvent);
        verify(processedEventChecker).markProcessed(sampleEvent.getEventId());
    }

    @Test
    void skipsConsumerWhenAlreadyProcessed() {
        when(processedEventChecker.isProcessed(sampleEvent.getEventId())).thenReturn(true);

        dispatcher.dispatch("raw-json");

        verify(consumer, never()).consume(any());
        verify(processedEventChecker, never()).markProcessed(any());
    }

    @Test
    void doesNotMarkProcessedWhenConsumerThrows() {
        when(processedEventChecker.isProcessed(sampleEvent.getEventId())).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("falha de negocio")).when(consumer).consume(sampleEvent);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> dispatcher.dispatch("raw-json"))).hasMessage("falha de negocio");

        verify(processedEventChecker, times(1)).isProcessed(sampleEvent.getEventId());
        verify(processedEventChecker, never()).markProcessed(any());
    }
}
