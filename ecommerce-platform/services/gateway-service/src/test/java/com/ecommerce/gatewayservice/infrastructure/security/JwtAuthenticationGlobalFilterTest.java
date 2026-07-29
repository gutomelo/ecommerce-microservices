package com.ecommerce.gatewayservice.infrastructure.security;

import com.ecommerce.gatewayservice.config.GatewaySecurityProperties;
import com.ecommerce.platform.security.JwtProperties;
import com.ecommerce.platform.security.JwtTokenProvider;
import com.ecommerce.platform.security.Roles;
import com.ecommerce.platform.security.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationGlobalFilterTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters!!";

    private JwtTokenProvider jwtTokenProvider;
    private GatewaySecurityProperties properties;
    private JwtAuthenticationGlobalFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        properties = new GatewaySecurityProperties();
        filter = new JwtAuthenticationGlobalFilter(jwtTokenProvider, properties);
    }

    @Test
    void bypassesAuthenticationForPublicPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsRequestWithoutTokenOnProtectedPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void rejectsRequestWithInvalidToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + "not-a-valid-jwt")
                .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void forwardsUserIdentityHeadersForValidToken() {
        String token = jwtTokenProvider.generateAccessToken("jane@example.com", Roles.ADMIN);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/orders")
                .header(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + token)
                .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        var captor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        var forwardedRequest = captor.getValue().getRequest();
        assertThat(forwardedRequest.getHeaders().getFirst(JwtAuthenticationGlobalFilter.USER_ID_HEADER))
                .isEqualTo("jane@example.com");
        assertThat(forwardedRequest.getHeaders().getFirst(JwtAuthenticationGlobalFilter.USER_ROLE_HEADER))
                .isEqualTo("ADMIN");
    }

    @Test
    void hasHigherOrderThanCorrelationFilter() {
        assertThat(filter.getOrder())
                .isGreaterThan(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}
