package com.ecommerce.platform.messaging;

/**
 * Abstrai a desserializacao de mensagens JSON recebidas via SQS de volta para o
 * tipo de evento concreto (ver .claude/rules/comunicacao-eventos.md).
 */
public interface MessageDeserializer {

    <T> T deserialize(String json, Class<T> type);
}
