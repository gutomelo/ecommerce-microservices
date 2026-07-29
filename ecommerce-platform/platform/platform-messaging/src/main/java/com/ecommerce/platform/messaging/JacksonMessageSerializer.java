package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonMessageSerializer implements MessageSerializer {

    private final ObjectMapper objectMapper;

    public JacksonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new MessageSerializationException("Falha ao serializar mensagem: " + value.getClass(), e);
        }
    }
}
