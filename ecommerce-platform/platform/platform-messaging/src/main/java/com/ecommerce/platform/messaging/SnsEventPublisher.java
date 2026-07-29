package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;
import com.ecommerce.platform.exception.IntegrationException;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
 * e Circuit Breaker conforme .claude/rules/resiliencia.md. Reutilizada por todos
 * os microsservicos publicadores - nenhum servico deve chamar SnsTemplate
 * diretamente.
 */
public class SnsEventPublisher implements EventPublisher {

    private static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(SnsEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final MessageSerializer serializer;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public SnsEventPublisher(SnsTemplate snsTemplate, MessageSerializer serializer, MessagingProperties properties) {
        this.snsTemplate = snsTemplate;
        this.serializer = serializer;
        this.retry = buildRetry(properties.getPublishRetry());
        this.circuitBreaker = buildCircuitBreaker(properties.getPublishCircuitBreaker());
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
        Supplier<Void> rawCall = () -> {
            Message<String> message = MessageBuilder.withPayload(jsonPayload)
                    .setHeader(EVENT_TYPE_HEADER, eventType)
                    .build();
            snsTemplate.send(topic, message);
            return null;
        };
        // CircuitBreaker envolve o Retry (nao o contrario): cada sequencia
        // completa de tentativas conta como UMA chamada na janela do circuit
        // breaker, e quando o circuito esta aberto a chamada falha
        // imediatamente, sem sequer tentar o retry (ver MessagingProperties).
        Supplier<Void> publishCall = CircuitBreaker.decorateSupplier(circuitBreaker,
                Retry.decorateSupplier(retry, rawCall));

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

    private CircuitBreaker buildCircuitBreaker(MessagingProperties.PublishCircuitBreaker config) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(config.getSlidingWindowSize())
                .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
                .failureRateThreshold(config.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(config.getWaitDurationInOpenStateMillis()))
                .permittedNumberOfCallsInHalfOpenState(config.getPermittedNumberOfCallsInHalfOpenState())
                .build();
        return CircuitBreaker.of("sns-publish", circuitBreakerConfig);
    }
}
