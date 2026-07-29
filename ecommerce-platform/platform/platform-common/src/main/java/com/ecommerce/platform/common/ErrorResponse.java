package com.ecommerce.platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Envelope padrao de resposta de erro para todos os endpoints REST da plataforma.
 * Construido pelo exception handler global de platform-exception.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, String correlationId,
                                    List<FieldViolation> violations) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, violations);
    }
}
