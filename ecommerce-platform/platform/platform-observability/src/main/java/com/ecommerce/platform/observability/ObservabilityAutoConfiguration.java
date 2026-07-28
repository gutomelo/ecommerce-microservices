package com.ecommerce.platform.observability;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Registra automaticamente o {@link CorrelationIdFilter} em qualquer microsservico
 * web que dependa de platform-observability, com a maior precedencia possivel
 * (precisa rodar antes de qualquer outro filtro para que os logs de toda a
 * requisicao, inclusive os de seguranca, ja carreguem o correlationId/traceId).
 * <p>
 * Guarda por {@code HttpServletRequest} (nao por {@code OncePerRequestFilter}):
 * spring-web e compartilhado entre Servlet e WebFlux, entao OncePerRequestFilter.class
 * sempre "existe" no classpath do gateway-service (reativo) mesmo sem servlet-api -
 * so falharia ao ser efetivamente carregada. HttpServletRequest so existe quando
 * ha de fato um container servlet (ver .claude/rules/estrutura-microsservico.md).
 */
@AutoConfiguration
@ConditionalOnClass(HttpServletRequest.class)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter(ObjectProvider<Tracer> tracerProvider) {
        return new CorrelationIdFilter(tracerProvider.getIfAvailable());
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
