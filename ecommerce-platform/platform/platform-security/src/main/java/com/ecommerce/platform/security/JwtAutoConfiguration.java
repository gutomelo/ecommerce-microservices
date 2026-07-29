package com.ecommerce.platform.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registra automaticamente {@link JwtTokenProvider} em qualquer microsservico que
 * dependa de platform-security, desde que {@code platform.security.jwt.secret}
 * esteja configurado. JwtTokenProvider nao tem nenhuma dependencia de Servlet,
 * entao e seguro tambem para o gateway-service (WebFlux).
 * <p>
 * {@link JwtAuthenticationFilter} (Servlet) fica em {@link JwtServletAutoConfiguration},
 * uma classe SEPARADA: nao basta um {@code @ConditionalOnClass} no metodo, pois
 * Class.getDeclaredMethods() falha ao introspectar a classe INTEIRA se qualquer
 * metodo referenciar um tipo Servlet ausente do classpath (WebFlux puro nao tem
 * jakarta.servlet-api) - o guard precisa estar na classe, nao no metodo (ver
 * .claude/rules/estrutura-microsservico.md).
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
        return new JwtTokenProvider(properties);
    }
}
