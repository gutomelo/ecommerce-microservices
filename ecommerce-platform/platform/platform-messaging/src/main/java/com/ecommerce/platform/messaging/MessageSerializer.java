package com.ecommerce.platform.messaging;

/**
 * Abstrai a serializacao de eventos para JSON. Nenhum microsservico deve usar
 * Jackson diretamente para (de)serializar eventos - sempre atraves desta interface
 * (ver .claude/rules/comunicacao-eventos.md).
 */
public interface MessageSerializer {

    String serialize(Object value);
}
