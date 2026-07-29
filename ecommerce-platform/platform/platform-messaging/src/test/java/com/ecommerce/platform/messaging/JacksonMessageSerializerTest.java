package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonMessageSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final JacksonMessageSerializer serializer = new JacksonMessageSerializer(objectMapper);

    record Sample(String name, int quantity) {
    }

    @Test
    void serializesObjectToJson() {
        String json = serializer.serialize(new Sample("widget", 3));

        assertThat(json).contains("\"widget\"").contains("\"quantity\":3");
    }

    static class Broken {
        public String getValue() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void wrapsFailuresInMessageSerializationException() {
        assertThatThrownBy(() -> serializer.serialize(new Broken()))
                .isInstanceOf(MessageSerializationException.class);
    }
}
