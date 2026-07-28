package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonMessageDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final JacksonMessageDeserializer deserializer = new JacksonMessageDeserializer(objectMapper);

    record Sample(String name, int quantity) {
    }

    @Test
    void deserializesJsonToObject() {
        Sample sample = deserializer.deserialize("{\"name\":\"widget\",\"quantity\":3}", Sample.class);

        assertThat(sample).isEqualTo(new Sample("widget", 3));
    }

    @Test
    void wrapsFailuresInMessageSerializationException() {
        assertThatThrownBy(() -> deserializer.deserialize("{not-valid", Sample.class))
                .isInstanceOf(MessageSerializationException.class);
    }
}
