package com.ecommerce.platform.observability;

import com.ecommerce.platform.common.constants.CorrelationHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesCorrelationIdFromIncomingHeader() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter(null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).thenReturn("existing-corr-id");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationHeaders.CORRELATION_ID_HEADER, "existing-corr-id");
        verify(chain).doFilter(request, response);
        assertThat(MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter(null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq(CorrelationHeaders.CORRELATION_ID_HEADER), anyString());
    }

    @Test
    void mdcHoldsCorrelationAndTraceIdDuringChainExecution() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-abc");

        CorrelationIdFilter filter = new CorrelationIdFilter(tracer);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).thenReturn("corr-1");

        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isEqualTo("corr-1");
            assertThat(MDC.get(CorrelationHeaders.TRACE_ID_MDC_KEY)).isEqualTo("trace-abc");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationHeaders.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenChainThrows() {
        CorrelationIdFilter filter = new CorrelationIdFilter(null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).thenReturn("corr-err");

        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () ->
                filter.doFilter(request, response, (req, res) -> {
                    throw new java.io.IOException("boom");
                }));

        assertThat(MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void doesNotFailWhenNoCurrentSpan() throws Exception {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        CorrelationIdFilter filter = new CorrelationIdFilter(tracer);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(CorrelationHeaders.CORRELATION_ID_HEADER)).thenReturn("corr-2");

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(CorrelationHeaders.TRACE_ID_MDC_KEY)).isNull();
    }
}
