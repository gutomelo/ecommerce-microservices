package com.ecommerce.platform.observability;

import com.ecommerce.platform.common.constants.CorrelationHeaders;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garante que toda requisicao tenha um correlationId (reaproveitado do header
 * recebido, ou gerado se ausente) e o traceId da span OpenTelemetry atual,
 * ambos disponiveis no MDC para todos os logs da requisicao (ver
 * .claude/rules/observabilidade.md). Idem para eventos publicados/consumidos
 * via platform-messaging, que propagam o mesmo correlationId.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public CorrelationIdFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);

        MDC.put(CorrelationHeaders.CORRELATION_ID_MDC_KEY, correlationId);
        putTraceIdInMdc();
        response.setHeader(CorrelationHeaders.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationHeaders.CORRELATION_ID_MDC_KEY);
            MDC.remove(CorrelationHeaders.TRACE_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String existing = request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER);
        return (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;
    }

    private void putTraceIdInMdc() {
        if (tracer == null) {
            return;
        }
        var currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            MDC.put(CorrelationHeaders.TRACE_ID_MDC_KEY, currentSpan.context().traceId());
        }
    }
}
