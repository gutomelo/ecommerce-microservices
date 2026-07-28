package com.ecommerce.platform.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Wrapper fino sobre Jackson para (de)serializacao JSON consistente em toda a
 * plataforma (usado, por exemplo, ao gravar o payload de eventos na tabela outbox).
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonProcessingRuntimeException("Falha ao serializar objeto para JSON: " + value.getClass(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new JsonProcessingRuntimeException("Falha ao desserializar JSON para " + type, e);
        }
    }

    public static class JsonProcessingRuntimeException extends RuntimeException {
        public JsonProcessingRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
