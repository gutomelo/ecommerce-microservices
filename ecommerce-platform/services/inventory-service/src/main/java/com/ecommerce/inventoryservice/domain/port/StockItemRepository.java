package com.ecommerce.inventoryservice.domain.port;

import com.ecommerce.inventoryservice.domain.StockItem;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository {

    Optional<StockItem> findByProductId(UUID productId);

    StockItem save(StockItem stockItem);
}
