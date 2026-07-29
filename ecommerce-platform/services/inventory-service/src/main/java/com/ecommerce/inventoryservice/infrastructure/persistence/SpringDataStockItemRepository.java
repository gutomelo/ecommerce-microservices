package com.ecommerce.inventoryservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataStockItemRepository extends JpaRepository<StockItemEntity, UUID> {
}
