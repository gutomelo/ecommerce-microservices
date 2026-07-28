package com.ecommerce.platform.exception;

/**
 * Erro de validacao de regra de negocio (nao confundir com validacao de bean/DTO,
 * que ja e tratada automaticamente pelo {@link GlobalExceptionHandler}).
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
