package com.ecommerce.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registra {@link JwtAuthenticationFilter} (Servlet) apenas quando ha um container
 * servlet no classpath. Classe separada de {@link JwtAutoConfiguration} de proposito
 * (ver o motivo la) - o guard por classe evita que o gateway-service (WebFlux)
 * quebre ao tentar introspectar este bean.
 */
@AutoConfiguration
@AutoConfigureAfter(JwtAutoConfiguration.class)
@ConditionalOnClass(HttpServletRequest.class)
public class JwtServletAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
}
