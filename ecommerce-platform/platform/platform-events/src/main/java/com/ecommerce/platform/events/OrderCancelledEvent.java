package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado por order-service quando o pedido e cancelado (estoque indisponivel
 * ou pagamento recusado). Dispara a compensacao. Consumido por inventory-service
 * (libera estoque) e notification-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCancelledEvent extends BaseEvent<OrderCancelledEvent.Payload> {

    public static final String EVENT_TYPE = "OrderCancelled";
    public static final String AGGREGATE_TYPE = "Order";

    public static OrderCancelledEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return OrderCancelledEvent.builder()
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

    public record Payload(UUID orderId, UUID customerId, String reason, Instant cancelledAt) {
    }
}
