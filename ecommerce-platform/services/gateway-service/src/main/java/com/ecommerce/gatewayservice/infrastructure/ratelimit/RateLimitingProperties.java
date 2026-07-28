package com.ecommerce.gatewayservice.infrastructure.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Rate limiting em memoria (Resilience4j) por IP de cliente - ver
 * .claude/rules/resiliencia.md. Sem Redis: cada instancia do gateway tem seu
 * proprio limite (aceitavel para o escopo deste projeto de portfolio).
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class RateLimitingProperties {

    private int limitForPeriod = 20;
    private long limitRefreshPeriodMillis = 1000;
}
