package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEventTest {

    @Getter
    @SuperBuilder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SampleEvent extends BaseEvent<String> {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void builderPopulatesAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-28T12:00:00Z");

        SampleEvent event = SampleEvent.builder()
                .eventId(eventId)
                .aggregateId("order-1")
                .aggregateType("Order")
                .eventType("Sample")
                .occurredAt(now)
                .correlationId("corr-1")
                .traceId("trace-1")
                .version(1)
                .payload("hello")
                .build();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getAggregateId()).isEqualTo("order-1");
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getEventType()).isEqualTo("Sample");
        assertThat(event.getOccurredAt()).isEqualTo(now);
        assertThat(event.getCorrelationId()).isEqualTo("corr-1");
        assertThat(event.getTraceId()).isEqualTo("trace-1");
        assertThat(event.getVersion()).isEqualTo(1);
        assertThat(event.getPayload()).isEqualTo("hello");
    }

    @Test
    void equalsAndHashCodeAreBasedOnAllFields() {
        SampleEvent a = baseBuilder().build();
        SampleEvent b = baseBuilder().build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("Sample");
    }

    @Test
    void jsonRoundTripPreservesAllFields() throws Exception {
        SampleEvent original = baseBuilder().build();

        String json = MAPPER.writeValueAsString(original);
        SampleEvent parsed = MAPPER.readValue(json, SampleEvent.class);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void jsonIgnoresUnknownProperties() throws Exception {
        String json = MAPPER.writeValueAsString(baseBuilder().build())
                .replaceFirst("\\{", "{\"unknownField\":\"ignored\",");

        SampleEvent parsed = MAPPER.readValue(json, SampleEvent.class);

        assertThat(parsed.getPayload()).isEqualTo("hello");
    }

    private SampleEvent.SampleEventBuilder<?, ?> baseBuilder() {
        return SampleEvent.builder()
                .eventId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .aggregateId("order-1")
                .aggregateType("Order")
                .eventType("Sample")
                .occurredAt(Instant.parse("2026-07-28T12:00:00Z"))
                .correlationId("corr-1")
                .traceId("trace-1")
                .version(1)
                .payload("hello");
    }
}
