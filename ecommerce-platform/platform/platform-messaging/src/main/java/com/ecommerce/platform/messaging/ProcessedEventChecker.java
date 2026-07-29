package com.ecommerce.platform.messaging;

import java.util.UUID;

/**
 * Porta do Idempotent Consumer Pattern (ver .claude/rules/comunicacao-eventos.md).
 * Cada microsservico implementa esta interface sobre sua propria tabela
 * {@code processed_events} - platform-messaging nao acessa banco de dados.
 */
public interface ProcessedEventChecker {

    boolean isProcessed(UUID eventId);

    void markProcessed(UUID eventId);
}
