package com.ecommerce.inventoryservice.infrastructure.persistence;

import com.ecommerce.inventoryservice.domain.StockReservation;
import com.ecommerce.inventoryservice.domain.port.StockReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Reservas sao imutaveis apos criadas (nunca atualizadas, so lidas para
 * compensacao) - por isso este adapter so implementa save (insert) e leitura,
 * sem o caso de update presente em OrderRepositoryAdapter.
 */
@Component
public class StockReservationRepositoryAdapter implements StockReservationRepository {

    private final SpringDataStockReservationRepository springDataStockReservationRepository;

    public StockReservationRepositoryAdapter(SpringDataStockReservationRepository springDataStockReservationRepository) {
        this.springDataStockReservationRepository = springDataStockReservationRepository;
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.setOrderId(reservation.getOrderId());
        entity.setCreatedAt(Instant.now());
        reservation.getItems().forEach(item -> {
            StockReservationItemEntity itemEntity = new StockReservationItemEntity();
            itemEntity.setReservation(entity);
            itemEntity.setProductId(item.productId());
            itemEntity.setQuantity(item.quantity());
            entity.getItems().add(itemEntity);
        });
        return toDomain(springDataStockReservationRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockReservation> findByOrderId(UUID orderId) {
        return springDataStockReservationRepository.findByOrderId(orderId).map(this::toDomain);
    }

    private StockReservation toDomain(StockReservationEntity entity) {
        var items = entity.getItems().stream()
                .map(i -> new StockReservation.ReservedItem(i.getProductId(), i.getQuantity()))
                .toList();
        return StockReservation.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .items(items)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
