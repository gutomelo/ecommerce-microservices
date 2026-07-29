package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonMessageDeserializer implements MessageDeserializer {

    private final ObjectMapper objectMapper;

    public JacksonMessageDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new MessageSerializationException("Falha ao desserializar mensagem para " + type, e);
        }
    }
}
