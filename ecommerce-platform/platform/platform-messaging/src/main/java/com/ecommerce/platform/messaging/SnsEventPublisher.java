package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;
import com.ecommerce.platform.exception.IntegrationException;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Implementacao unica de {@link EventPublisher}, com Retry (backoff exponencial)
 * conforme .claude/rules/resiliencia.md. Reutilizada por todos os microsservicos
 * publicadores - nenhum servico deve chamar SnsTemplate diretamente.
 */
public class SnsEventPublisher implements EventPublisher {

    private static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(SnsEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final MessageSerializer serializer;
    private final Retry retry;

    public SnsEventPublisher(SnsTemplate snsTemplate, MessageSerializer serializer, MessagingProperties properties) {
        this.snsTemplate = snsTemplate;
        this.serializer = serializer;
        this.retry = buildRetry(properties.getPublishRetry());
    }

    @Override
    public void publish(String topic, BaseEvent<?> event) {
        String json = serializer.serialize(event);
        doPublish(topic, event.getEventType(), json);
    }

    @Override
    public void publishRaw(String topic, String eventType, String jsonPayload) {
        doPublish(topic, eventType, jsonPayload);
    }

    private void doPublish(String topic, String eventType, String jsonPayload) {
        Supplier<Void> publishCall = Retry.decorateSupplier(retry, () -> {
            Message<String> message = MessageBuilder.withPayload(jsonPayload)
                    .setHeader(EVENT_TYPE_HEADER, eventType)
                    .build();
            snsTemplate.send(topic, message);
            return null;
        });

        try {
            publishCall.get();
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} no topico {} apos retries", eventType, topic, e);
            throw new IntegrationException(
                    "Falha ao publicar evento %s no topico %s".formatted(eventType, topic), e);
        }
    }

    private Retry buildRetry(MessagingProperties.PublishRetry config) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getMaxAttempts())
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(config.getInitialIntervalMillis()), config.getMultiplier()))
                .retryExceptions(Exception.class)
                .build();
        return Retry.of("sns-publish", retryConfig);
    }
}
