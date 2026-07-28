package com.ecommerce.platform.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuracao de retry para publicacao no SNS (ver .claude/rules/resiliencia.md).
 * O retry de consumo (SQS) e controlado pela propria fila (maxReceiveCount +
 * visibility timeout), configurada em infrastructure/localstack.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.messaging")
public class MessagingProperties {

    @NestedConfigurationProperty
    private PublishRetry publishRetry = new PublishRetry();

    @Getter
    @Setter
    public static class PublishRetry {
        private int maxAttempts = 3;
        private long initialIntervalMillis = 200;
        private double multiplier = 2.0;
    }
}
