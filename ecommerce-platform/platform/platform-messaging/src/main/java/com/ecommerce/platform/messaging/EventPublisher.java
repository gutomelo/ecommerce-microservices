package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.BaseEvent;

/**
 * Unica forma de publicar um evento de dominio no SNS. Nenhum microsservico deve
 * falar diretamente com o SDK da AWS (ver .claude/rules/comunicacao-eventos.md).
 */
public interface EventPublisher {

    /**
     * Serializa e publica o evento no topico informado (nome ou ARN).
     */
    void publish(String topic, BaseEvent<?> event);

    /**
     * Republica um payload ja serializado (usado pelo worker do Outbox Pattern,
     * que le a linha da tabela outbox e nao precisa reconstruir o objeto Java).
     */
    void publishRaw(String topic, String eventType, String jsonPayload);
}
