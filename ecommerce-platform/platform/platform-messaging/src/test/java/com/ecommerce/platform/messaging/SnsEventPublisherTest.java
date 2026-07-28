package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.exception.IntegrationException;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SnsEventPublisherTest {

    private SnsTemplate snsTemplate;
    private MessageSerializer serializer;
    private MessagingProperties properties;

    @BeforeEach
    void setUp() {
        snsTemplate = mock(SnsTemplate.class);
        serializer = new JacksonMessageSerializer(
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));

        properties = new MessagingProperties();
        properties.getPublishRetry().setMaxAttempts(3);
        properties.getPublishRetry().setInitialIntervalMillis(1);
        properties.getPublishRetry().setMultiplier(1.0);
    }

    private OrderCreatedEvent sampleEvent() {
        var payload = new OrderCreatedEvent.Payload(
                UUID.randomUUID(), UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 1, new BigDecimal("10.00"))),
                new BigDecimal("10.00"));
        return OrderCreatedEvent.of(UUID.randomUUID().toString(), "corr-1", "trace-1", payload);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesSuccessfullyOnFirstAttempt() {
        SnsEventPublisher publisher = new SnsEventPublisher(snsTemplate, serializer, properties);
        doNothing().when(snsTemplate).send(anyString(), any(Message.class));

        publisher.publish("OrderCreated", sampleEvent());

        verify(snsTemplate, times(1)).send(eq("OrderCreated"), any(Message.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void retriesAndSucceedsAfterTransientFailure() {
        SnsEventPublisher publisher = new SnsEventPublisher(snsTemplate, serializer, properties);
        doThrow(new RuntimeException("timeout"))
                .doNothing()
                .when(snsTemplate).send(anyString(), any(Message.class));

        publisher.publish("OrderCreated", sampleEvent());

        verify(snsTemplate, times(2)).send(eq("OrderCreated"), any(Message.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void throwsIntegrationExceptionAfterExhaustingRetries() {
        SnsEventPublisher publisher = new SnsEventPublisher(snsTemplate, serializer, properties);
        doThrow(new RuntimeException("indisponivel"))
                .when(snsTemplate).send(anyString(), any(Message.class));

        assertThatThrownBy(() -> publisher.publish("OrderCreated", sampleEvent()))
                .isInstanceOf(IntegrationException.class);

        verify(snsTemplate, times(3)).send(eq("OrderCreated"), any(Message.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishRawSendsPayloadVerbatim() {
        SnsEventPublisher publisher = new SnsEventPublisher(snsTemplate, serializer, properties);
        doNothing().when(snsTemplate).send(anyString(), any(Message.class));

        publisher.publishRaw("OrderCreated", "OrderCreated", "{\"raw\":true}");

        verify(snsTemplate, times(1)).send(eq("OrderCreated"), any(Message.class));
    }
}
