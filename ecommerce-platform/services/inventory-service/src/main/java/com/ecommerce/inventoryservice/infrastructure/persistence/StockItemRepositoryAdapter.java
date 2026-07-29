package com.ecommerce.inventoryservice.infrastructure.persistence;

import com.ecommerce.inventoryservice.domain.StockItem;
import com.ecommerce.inventoryservice.domain.port.StockItemRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StockItemRepositoryAdapter implements StockItemRepository {

    private final SpringDataStockItemRepository springDataStockItemRepository;

    public StockItemRepositoryAdapter(SpringDataStockItemRepository springDataStockItemRepository) {
        this.springDataStockItemRepository = springDataStockItemRepository;
    }

    @Override
    public Optional<StockItem> findByProductId(UUID productId) {
        return springDataStockItemRepository.findById(productId).map(this::toDomain);
    }

    @Override
    public StockItem save(StockItem stockItem) {
        StockItemEntity entity = new StockItemEntity();
        entity.setProductId(stockItem.getProductId());
        entity.setAvailableQuantity(stockItem.getAvailableQuantity());
        return toDomain(springDataStockItemRepository.save(entity));
    }

    private StockItem toDomain(StockItemEntity entity) {
        return StockItem.builder()
                .productId(entity.getProductId())
                .availableQuantity(entity.getAvailableQuantity())
                .build();
    }
}
