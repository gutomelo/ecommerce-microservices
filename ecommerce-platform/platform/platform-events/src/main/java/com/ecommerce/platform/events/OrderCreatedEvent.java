package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publicado por order-service ao criar um novo pedido (PENDING). Dispara a Saga.
 * Consumido por inventory-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent extends BaseEvent<OrderCreatedEvent.Payload> {

    public static final String EVENT_TYPE = "OrderCreated";
    public static final String AGGREGATE_TYPE = "Order";

    public static OrderCreatedEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateId(orderId)
                .aggregateType(AGGREGATE_TYPE)
                .eventType(EVENT_TYPE)
                .occurredAt(Instant.now())
                .correlationId(correlationId)
                .traceId(traceId)
                .version(1)
                .payload(payload)
                .build();
    }

    public record Payload(UUID orderId, UUID customerId, List<Item> items, BigDecimal totalAmount) {

        public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
        }
    }
}
