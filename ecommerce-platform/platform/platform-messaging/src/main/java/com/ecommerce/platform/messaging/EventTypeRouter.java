package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Roteia uma mensagem crua (JSON) para o handler certo com base no campo
 * eventType, sem que cada microsservico reimplemente esse "peek" manualmente.
 * Necessario porque uma unica fila SQS recebe varios tipos de evento (fan-out de
 * multiplos topicos SNS - ver docs/events/catalogo-eventos.md). Cada handler
 * registrado normalmente e um IdempotentEventDispatcher::dispatch.
 */
public class EventTypeRouter {

    private static final Logger log = LoggerFactory.getLogger(EventTypeRouter.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Consumer<String>> routes = new HashMap<>();

    public EventTypeRouter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventTypeRouter register(String eventType, Consumer<String> handler) {
        routes.put(eventType, handler);
        return this;
    }

    public void route(String rawMessageBody) {
        String eventType = extractEventType(rawMessageBody);
        Consumer<String> handler = routes.get(eventType);
        if (handler == null) {
            log.warn("Nenhum handler registrado para eventType={} - mensagem ignorada", eventType);
            return;
        }
        handler.accept(rawMessageBody);
    }

    private String extractEventType(String rawMessageBody) {
        try {
            JsonNode node = objectMapper.readTree(rawMessageBody);
            return node.path("eventType").asText(null);
        } catch (Exception e) {
            throw new MessageSerializationException("Falha ao extrair eventType da mensagem", e);
        }
    }
}
