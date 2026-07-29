package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publicado por inventory-service quando nao ha estoque suficiente para um ou
 * mais itens do pedido. Dispara a compensacao da Saga. Consumido por order-service.
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockUnavailableEvent extends BaseEvent<StockUnavailableEvent.Payload> {

    public static final String EVENT_TYPE = "StockUnavailable";
    public static final String AGGREGATE_TYPE = "Stock";

    public static StockUnavailableEvent of(String orderId, String correlationId, String traceId, Payload payload) {
        return StockUnavailableEvent.builder()
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

    public record Payload(UUID orderId, List<UUID> unavailableProductIds, String reason) {
    }
}
