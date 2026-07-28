package com.ecommerce.platform.exception;

import com.ecommerce.platform.common.ErrorResponse;
import com.ecommerce.platform.common.constants.CorrelationHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.MDC;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getMethod()).thenReturn("GET");
        return request;
    }

    @BeforeEach
    void setCorrelationId() {
        MDC.put(CorrelationHeaders.CORRELATION_ID_MDC_KEY, "corr-test");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void handlesResourceNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                ResourceNotFoundException.forId("Order", "1"), request("/orders/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().correlationId()).isEqualTo("corr-test");
        assertThat(response.getBody().path()).isEqualTo("/orders/1");
    }

    @Test
    void handlesConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new ConflictException("ja confirmado"), request("/orders/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handlesValidation() {
        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new ValidationException("invalido"), request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesUnauthorized() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("sem token"), request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handlesForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("sem permissao"), request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handlesIntegrationFailure() {
        ResponseEntity<ErrorResponse> response = handler.handleIntegration(
                new IntegrationException("falha no SNS"), request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handlesGenericBusinessException() {
        var generic = new ValidationException("erro generico") {
        };
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(generic, request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handlesBeanValidationErrors() throws NoSuchMethodException {
        Method method = SampleTarget.class.getMethod("create", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleTarget(), "sampleTarget");
        bindingResult.addError(new FieldError("sampleTarget", "name", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleBeanValidation(ex, request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().violations()).hasSize(1);
        assertThat(response.getBody().violations().get(0).field()).isEqualTo("name");
    }

    @Test
    void handlesUnexpectedException() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"), request("/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Erro interno inesperado");
    }

    static class SampleTarget {
        public void create(String name) {
        }
    }
}
