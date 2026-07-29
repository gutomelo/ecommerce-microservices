package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;

/**
 * Porta implementada pela camada de aplicacao de cada microsservico para reagir
 * a um evento de dominio ja desserializado e verificado quanto a idempotencia.
 * Mantem a logica de negocio isolada dos detalhes de SQS (ver
 * .claude/rules/comunicacao-eventos.md e .claude/rules/arquitetura.md).
 */
public interface EventConsumer<T extends BaseEvent<?>> {

    void consume(T event);
}
