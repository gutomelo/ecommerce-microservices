package com.ecommerce.orderservice.application.port;

import com.ecommerce.platform.events.BaseEvent;

/**
 * Porta do Outbox Pattern (ver .claude/rules/comunicacao-eventos.md): grava o
 * evento na mesma transacao da mudanca de estado do agregado. A publicacao real
 * no SNS acontece depois, de forma assincrona, via OutboxPublisherWorker.
 */
public interface OutboxEventStore {

    void store(BaseEvent<?> event, String topic);
}
