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

    /**
     * totalAmount viaja aqui (nao apenas em OrderCreatedEvent) porque
     * payment-service consome exclusivamente StockReservedEvent - nunca
     * OrderCreatedEvent - e precisa do valor do pedido para decidir
     * aprovacao/recusa sem acoplar-se a outro topico (ver docs/saga/fluxo-saga.md).
     */
    public record Payload(UUID orderId, List<ReservedItem> items, BigDecimal totalAmount) {

        public record ReservedItem(UUID productId, int quantity) {
        }
    }
}
