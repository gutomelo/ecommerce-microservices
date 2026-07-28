package com.ecommerce.gatewayservice.infrastructure.correlation;

import com.ecommerce.platform.common.constants.CorrelationHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void generatesCorrelationIdWhenMissingAndPropagatesDownstream() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotBlank();

        var captor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID_HEADER))
                .isEqualTo(responseHeader);
    }

    @Test
    void reusesIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login")
                        .header(CorrelationHeaders.CORRELATION_ID_HEADER, "corr-1")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationHeaders.CORRELATION_ID_HEADER))
                .isEqualTo("corr-1");
    }

    @Test
    void hasHighestPrecedenceOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
