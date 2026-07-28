package com.ecommerce.platform.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void ofWithoutViolations() {
        ErrorResponse error = ErrorResponse.of(404, "Not Found", "Pedido nao encontrado", "/orders/1", "corr-1");

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.message()).isEqualTo("Pedido nao encontrado");
        assertThat(error.path()).isEqualTo("/orders/1");
        assertThat(error.correlationId()).isEqualTo("corr-1");
        assertThat(error.violations()).isNull();
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    void ofWithViolations() {
        List<ErrorResponse.FieldViolation> violations = List.of(
                new ErrorResponse.FieldViolation("name", "must not be blank"));

        ErrorResponse error = ErrorResponse.of(400, "Bad Request", "Validacao falhou", "/orders", "corr-2", violations);

        assertThat(error.violations()).hasSize(1);
        assertThat(error.violations().get(0).field()).isEqualTo("name");
        assertThat(error.violations().get(0).message()).isEqualTo("must not be blank");
    }
}
