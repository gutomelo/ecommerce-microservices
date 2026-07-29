package com.ecommerce.inventoryservice.application;

import com.ecommerce.inventoryservice.application.port.OutboxEventStore;
import com.ecommerce.inventoryservice.domain.StockItem;
import com.ecommerce.inventoryservice.domain.StockReservation;
import com.ecommerce.inventoryservice.domain.port.StockItemRepository;
import com.ecommerce.inventoryservice.domain.port.StockReservationRepository;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private StockItemRepository stockItemRepository;
    private StockReservationRepository stockReservationRepository;
    private OutboxEventStore outboxEventStore;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        stockItemRepository = mock(StockItemRepository.class);
        stockReservationRepository = mock(StockReservationRepository.class);
        outboxEventStore = mock(OutboxEventStore.class);
        inventoryService = new InventoryService(stockItemRepository, stockReservationRepository, outboxEventStore);
        when(stockItemRepository.save(any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reserveStockReservesAllItemsAndPublishesStockReservedWhenSufficient() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(stockItemRepository.findByProductId(productId))
                .thenReturn(Optional.of(StockItem.builder().productId(productId).availableQuantity(10).build()));

        inventoryService.reserveStock(orderId, List.of(new RequestedItem(productId, 3)), new BigDecimal("39.80"),
                "corr-1", "trace-1");

        verify(stockItemRepository).save(argThat(item -> item.getAvailableQuantity() == 7));
        verify(stockReservationRepository).save(any(StockReservation.class));
        verify(outboxEventStore).store(any(StockReservedEvent.class), eq(StockReservedEvent.EVENT_TYPE));
        verify(outboxEventStore, never()).store(any(StockUnavailableEvent.class), any());
    }

    @Test
    void reserveStockProvisionsDefaultQuantityForUnknownProduct() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(stockItemRepository.findByProductId(productId)).thenReturn(Optional.empty());

        inventoryService.reserveStock(orderId, List.of(new RequestedItem(productId, 5)), new BigDecimal("10.00"),
                "corr-1", "trace-1");

        verify(stockItemRepository).save(argThat(item ->
                item.getAvailableQuantity() == StockItem.DEFAULT_INITIAL_QUANTITY - 5));
        verify(outboxEventStore).store(any(StockReservedEvent.class), eq(StockReservedEvent.EVENT_TYPE));
    }

    @Test
    void reserveStockPublishesStockUnavailableAndReservesNothingWhenAnyItemInsufficient() {
        UUID orderId = UUID.randomUUID();
        UUID sufficientProductId = UUID.randomUUID();
        UUID insufficientProductId = UUID.randomUUID();
        when(stockItemRepository.findByProductId(sufficientProductId))
                .thenReturn(Optional.of(StockItem.builder().productId(sufficientProductId).availableQuantity(10).build()));
        when(stockItemRepository.findByProductId(insufficientProductId))
                .thenReturn(Optional.of(StockItem.builder().productId(insufficientProductId).availableQuantity(1).build()));

        inventoryService.reserveStock(orderId,
                List.of(new RequestedItem(sufficientProductId, 2), new RequestedItem(insufficientProductId, 5)),
                new BigDecimal("10.00"), "corr-1", "trace-1");

        verify(stockItemRepository, never()).save(any());
        verify(stockReservationRepository, never()).save(any());
        verify(outboxEventStore).store(argThat(event -> {
            var payload = ((StockUnavailableEvent) event).getPayload();
            return payload.unavailableProductIds().equals(List.of(insufficientProductId));
        }), eq(StockUnavailableEvent.EVENT_TYPE));
    }

    @Test
    void releaseStockReleasesEachReservedItem() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var reservation = StockReservation.create(orderId, List.of(new StockReservation.ReservedItem(productId, 4)));
        when(stockReservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
        when(stockItemRepository.findByProductId(productId))
                .thenReturn(Optional.of(StockItem.builder().productId(productId).availableQuantity(6).build()));

        inventoryService.releaseStock(orderId);

        verify(stockItemRepository).save(argThat(item -> item.getAvailableQuantity() == 10));
    }

    @Test
    void releaseStockIsNoOpWhenNoReservationExistsForOrder() {
        UUID orderId = UUID.randomUUID();
        when(stockReservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        inventoryService.releaseStock(orderId);

        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void getStockLevelReturnsExistingItemWithoutPersisting() {
        UUID productId = UUID.randomUUID();
        when(stockItemRepository.findByProductId(productId))
                .thenReturn(Optional.of(StockItem.builder().productId(productId).availableQuantity(42).build()));

        StockItem result = inventoryService.getStockLevel(productId);

        assertThat(result.getAvailableQuantity()).isEqualTo(42);
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void getStockLevelProvisionsDefaultForUnknownProductWithoutPersisting() {
        UUID productId = UUID.randomUUID();
        when(stockItemRepository.findByProductId(productId)).thenReturn(Optional.empty());

        StockItem result = inventoryService.getStockLevel(productId);

        assertThat(result.getAvailableQuantity()).isEqualTo(StockItem.DEFAULT_INITIAL_QUANTITY);
        verify(stockItemRepository, never()).save(any());
    }
}
