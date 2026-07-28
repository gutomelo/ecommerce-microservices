package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publicado por inventory-service quando ha estoque suficiente para reservar
 * todos os itens de um pedido. Consumido por payment-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockReservedEvent extends BaseEvent<StockReservedEvent.Payload> {

    public static final String EVENT_TYPE = "StockReserved";
    public static final String AGGREGATE_TYPE = "Stock";

    public static StockReservedEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return StockReservedEvent.builder()
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

    public record Payload(UUID orderId, List<ReservedItem> items) {

        public record ReservedItem(UUID productId, int quantity) {
        }
    }
}
