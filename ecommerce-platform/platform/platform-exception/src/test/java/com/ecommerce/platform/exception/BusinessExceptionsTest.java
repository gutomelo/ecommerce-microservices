package com.ecommerce.platform.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionsTest {

    @Test
    void validationExceptionCarriesCodeAndMessage() {
        var ex = new ValidationException("campo invalido");

        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getMessage()).isEqualTo("campo invalido");
    }

    @Test
    void resourceNotFoundExceptionForId() {
        var ex = ResourceNotFoundException.forId("Order", "abc-123");

        assertThat(ex.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Order nao encontrado(a) para o id: abc-123");
    }

    @Test
    void conflictExceptionCarriesCode() {
        var ex = new ConflictException("pedido ja confirmado");

        assertThat(ex.getErrorCode()).isEqualTo("CONFLICT");
        assertThat(ex.getMessage()).isEqualTo("pedido ja confirmado");
    }

    @Test
    void unauthorizedExceptionCarriesCode() {
        var ex = new UnauthorizedException("token invalido");

        assertThat(ex.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void forbiddenExceptionCarriesCode() {
        var ex = new ForbiddenException("acesso negado");

        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void integrationExceptionWithoutCause() {
        var ex = new IntegrationException("falha ao publicar no SNS");

        assertThat(ex.getErrorCode()).isEqualTo("INTEGRATION_ERROR");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void integrationExceptionWithCause() {
        var cause = new RuntimeException("timeout");
        var ex = new IntegrationException("falha ao publicar no SNS", cause);

        assertThat(ex.getCause()).isSameAs(cause);
    }
}
