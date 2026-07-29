package com.ecommerce.platform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registra o {@link GlobalExceptionHandler} automaticamente em qualquer microsservico
 * que dependa de platform-exception. Sem isto, o handler nunca vira bean: seu pacote
 * (com.ecommerce.platform.exception) fica fora do component scan padrao de
 * @SpringBootApplication de cada servico (que so escaneia o proprio pacote base).
 * Guarda por HttpServletRequest pelo mesmo motivo de platform-security/platform-observability
 * (ver .claude/rules/estrutura-microsservico.md) - nao se aplica ao gateway-service (WebFlux).
 */
@AutoConfiguration
@ConditionalOnClass(HttpServletRequest.class)
public class ExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
