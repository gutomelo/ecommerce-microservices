package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado por order-service quando o pagamento e aprovado e o pedido passa
 * para CONFIRMED. Consumido por notification-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderConfirmedEvent extends BaseEvent<OrderConfirmedEvent.Payload> {

    public static final String EVENT_TYPE = "OrderConfirmed";
    public static final String AGGREGATE_TYPE = "Order";

    public static OrderConfirmedEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return OrderConfirmedEvent.builder()
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

    public record Payload(UUID orderId, UUID customerId, Instant confirmedAt) {
    }
}
