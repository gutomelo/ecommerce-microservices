package com.ecommerce.platform.common.constants;

/**
 * Nomes de header/MDC compartilhados para rastreabilidade ponta a ponta
 * (HTTP entre Gateway e servicos, e propagados para os eventos de platform-events).
 */
public final class CorrelationHeaders {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private CorrelationHeaders() {
    }
}
