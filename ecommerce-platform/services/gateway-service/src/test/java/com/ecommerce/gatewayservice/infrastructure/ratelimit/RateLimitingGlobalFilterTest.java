package com.ecommerce.gatewayservice.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitingGlobalFilterTest {

    private RateLimitingGlobalFilter newFilter(int limitForPeriod) {
        RateLimitingProperties properties = new RateLimitingProperties();
        properties.setLimitForPeriod(limitForPeriod);
        properties.setLimitRefreshPeriodMillis(60_000);
        return new RateLimitingGlobalFilter(properties);
    }

    private MockServerWebExchange requestFrom(String ip) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
                .remoteAddress(new InetSocketAddress(ip, 12345))
                .build());
    }

    @Test
    void allowsRequestsWithinLimit() {
        RateLimitingGlobalFilter filter = newFilter(2);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        var exchange = requestFrom("10.0.0.1");
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsRequestsAboveLimitWithTooManyRequests() {
        RateLimitingGlobalFilter filter = newFilter(1);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(requestFrom("10.0.0.2"), chain).block();
        var secondExchange = requestFrom("10.0.0.2");

        filter.filter(secondExchange, chain).block();

        assertThat(secondExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void tracksLimitsSeparatelyPerClientIp() {
        RateLimitingGlobalFilter filter = newFilter(1);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(requestFrom("10.0.0.3"), chain).block();
        var otherClientExchange = requestFrom("10.0.0.4");

        filter.filter(otherClientExchange, chain).block();

        assertThat(otherClientExchange.getResponse().getStatusCode()).isNull();
    }
}
