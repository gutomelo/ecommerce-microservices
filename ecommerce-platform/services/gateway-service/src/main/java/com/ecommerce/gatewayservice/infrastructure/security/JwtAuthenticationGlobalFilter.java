package com.ecommerce.gatewayservice.infrastructure.security;

import com.ecommerce.gatewayservice.config.GatewaySecurityProperties;
import com.ecommerce.platform.security.JwtTokenProvider;
import com.ecommerce.platform.security.Roles;
import com.ecommerce.platform.security.SecurityConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Valida o JWT na borda da plataforma antes de rotear para qualquer servico
 * protegido. /auth/** (login/registro/refresh) e /actuator/** ficam de fora
 * (ver GatewaySecurityProperties). Equivalente reativo de
 * platform-security.JwtAuthenticationFilter (Servlet) - reaproveita apenas
 * JwtTokenProvider, que nao tem dependencia de Servlet (ver
 * .claude/rules/estrutura-microsservico.md).
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtTokenProvider jwtTokenProvider;
    private final GatewaySecurityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationGlobalFilter(JwtTokenProvider jwtTokenProvider, GatewaySecurityProperties properties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null || !jwtTokenProvider.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String subject = jwtTokenProvider.getSubject(token);
        Roles role = jwtTokenProvider.getRole(token);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(USER_ID_HEADER, subject)
                .header(USER_ROLE_HEADER, role.name())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return properties.getPublicPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
