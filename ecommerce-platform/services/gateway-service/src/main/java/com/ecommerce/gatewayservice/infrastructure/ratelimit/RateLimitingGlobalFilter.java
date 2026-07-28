package com.ecommerce.gatewayservice.infrastructure.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Rate limiting por IP de cliente (ver .claude/rules/resiliencia.md), usando o
 * RateLimiter do Resilience4j de forma reativa (RateLimiterOperator) - sem
 * dependencia de Redis.
 */
@Component
public class RateLimitingGlobalFilter implements GlobalFilter, Ordered {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitingGlobalFilter(RateLimitingProperties properties) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getLimitForPeriod())
                .limitRefreshPeriod(Duration.ofMillis(properties.getLimitRefreshPeriodMillis()))
                .timeoutDuration(Duration.ZERO)
                .build();
        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(resolveClientKey(exchange));

        return chain.filter(exchange)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .onErrorResume(RequestNotPermitted.class, ex -> {
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                });
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
