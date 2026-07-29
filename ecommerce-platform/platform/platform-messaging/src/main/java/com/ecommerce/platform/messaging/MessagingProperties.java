package com.ecommerce.platform.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuracao de retry e circuit breaker para publicacao no SNS (ver
 * .claude/rules/resiliencia.md). O retry de consumo (SQS) e controlado pela
 * propria fila (maxReceiveCount + visibility timeout), configurada em
 * infrastructure/localstack.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.messaging")
public class MessagingProperties {

    @NestedConfigurationProperty
    private PublishRetry publishRetry = new PublishRetry();

    @NestedConfigurationProperty
    private PublishCircuitBreaker publishCircuitBreaker = new PublishCircuitBreaker();

    @Getter
    @Setter
    public static class PublishRetry {
        private int maxAttempts = 3;
        private long initialIntervalMillis = 200;
        private double multiplier = 2.0;
    }

    /**
     * Protege o SNS de chamadas repetidas quando ja se sabe que ele esta
     * indisponivel (falha em cascata) - ver .claude/rules/resiliencia.md. Envolve
     * o Retry (nao o contrario): cada sequencia completa de retry conta como UMA
     * chamada na janela do circuit breaker, evitando abrir o circuito por causa
     * das proprias tentativas internas do retry.
     */
    @Getter
    @Setter
    public static class PublishCircuitBreaker {
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private float failureRateThreshold = 50f;
        private long waitDurationInOpenStateMillis = 10_000;
        private int permittedNumberOfCallsInHalfOpenState = 3;
    }
}
