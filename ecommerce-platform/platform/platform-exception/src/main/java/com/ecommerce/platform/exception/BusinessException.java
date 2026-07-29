package com.ecommerce.platform.exception;

/**
 * Excecao base para erros de regra de negocio esperados. Nunca deve ser lancada
 * diretamente; use uma das subclasses especificas (ver .claude/rules/modulo-platform.md).
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
