package com.ecommerce.inventoryservice.application;

import com.ecommerce.inventoryservice.application.port.OutboxEventStore;
import com.ecommerce.inventoryservice.domain.StockItem;
import com.ecommerce.inventoryservice.domain.StockReservation;
import com.ecommerce.inventoryservice.domain.port.StockItemRepository;
import com.ecommerce.inventoryservice.domain.port.StockReservationRepository;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reage a OrderCreatedEvent (reserva) e OrderCancelledEvent (compensacao,
 * ver .claude/rules/resiliencia.md). Reserva e tudo-ou-nada: se qualquer item
 * do pedido nao tiver estoque suficiente, nenhum item e reservado.
 */
@Service
public class InventoryService {

    private final StockItemRepository stockItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxEventStore outboxEventStore;

    public InventoryService(StockItemRepository stockItemRepository,
                             StockReservationRepository stockReservationRepository,
                             OutboxEventStore outboxEventStore) {
        this.stockItemRepository = stockItemRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.outboxEventStore = outboxEventStore;
    }

    @Transactional
    public void reserveStock(UUID orderId, List<RequestedItem> requestedItems, BigDecimal totalAmount,
                              String correlationId, String traceId) {
        List<StockItem> currentItems = requestedItems.stream()
                .map(item -> findOrProvision(item.productId()))
                .toList();

        List<UUID> unavailableProductIds = new ArrayList<>();
        for (int i = 0; i < requestedItems.size(); i++) {
            if (!currentItems.get(i).hasSufficientStock(requestedItems.get(i).quantity())) {
                unavailableProductIds.add(requestedItems.get(i).productId());
            }
        }

        if (!unavailableProductIds.isEmpty()) {
            publishStockUnavailable(orderId, unavailableProductIds, correlationId, traceId);
            return;
        }

        List<StockReservation.ReservedItem> reservedItems = new ArrayList<>();
        for (RequestedItem requested : requestedItems) {
            StockItem current = findOrProvision(requested.productId());
            stockItemRepository.save(current.reserve(requested.quantity()));
            reservedItems.add(new StockReservation.ReservedItem(requested.productId(), requested.quantity()));
        }

        stockReservationRepository.save(StockReservation.create(orderId, reservedItems));
        publishStockReserved(orderId, reservedItems, totalAmount, correlationId, traceId);
    }

    public StockItem getStockLevel(UUID productId) {
        return findOrProvision(productId);
    }

    @Transactional
    public void releaseStock(UUID orderId) {
        stockReservationRepository.findByOrderId(orderId).ifPresent(reservation ->
                reservation.getItems().forEach(item -> {
                    StockItem current = findOrProvision(item.productId());
                    stockItemRepository.save(current.release(item.quantity()));
                }));
    }

    private StockItem findOrProvision(UUID productId) {
        return stockItemRepository.findByProductId(productId)
                .orElseGet(() -> StockItem.provisionDefault(productId));
    }

    private void publishStockReserved(UUID orderId, List<StockReservation.ReservedItem> items, BigDecimal totalAmount,
                                       String correlationId, String traceId) {
        var eventItems = items.stream()
                .map(item -> new StockReservedEvent.Payload.ReservedItem(item.productId(), item.quantity()))
                .toList();
        var payload = new StockReservedEvent.Payload(orderId, eventItems, totalAmount);
        var event = StockReservedEvent.of(orderId.toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, StockReservedEvent.EVENT_TYPE);
    }

    private void publishStockUnavailable(UUID orderId, List<UUID> unavailableProductIds,
                                          String correlationId, String traceId) {
        var payload = new StockUnavailableEvent.Payload(orderId, unavailableProductIds, "estoque insuficiente");
        var event = StockUnavailableEvent.of(orderId.toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, StockUnavailableEvent.EVENT_TYPE);
    }
}
