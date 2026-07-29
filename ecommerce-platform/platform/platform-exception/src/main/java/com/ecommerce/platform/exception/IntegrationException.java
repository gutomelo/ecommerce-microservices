package com.ecommerce.platform.exception;

/**
 * Falha tecnica ao integrar com um sistema externo (SNS, SQS, outro servico via
 * Gateway). Diferente das demais, normalmente e elegivel para Retry (ver
 * .claude/rules/resiliencia.md) em vez de ser reportada diretamente ao cliente.
 */
public class IntegrationException extends BusinessException {

    public IntegrationException(String message) {
        super("INTEGRATION_ERROR", message);
    }

    public IntegrationException(String message, Throwable cause) {
        super("INTEGRATION_ERROR", message, cause);
    }
}
