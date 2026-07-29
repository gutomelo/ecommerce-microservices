package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Publicado por payment-service quando o pagamento do pedido e recusado.
 * Dispara a compensacao da Saga. Consumido por order-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDeclinedEvent extends BaseEvent<PaymentDeclinedEvent.Payload> {

    public static final String EVENT_TYPE = "PaymentDeclined";
    public static final String AGGREGATE_TYPE = "Payment";

    public static PaymentDeclinedEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return PaymentDeclinedEvent.builder()
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

    public record Payload(UUID orderId, BigDecimal amount, String reason) {
    }
}
