package com.ecommerce.inventoryservice.api.dto;

import com.ecommerce.inventoryservice.domain.StockItem;

import java.util.UUID;

public record StockItemResponse(UUID productId, int availableQuantity) {

    public static StockItemResponse from(StockItem stockItem) {
        return new StockItemResponse(stockItem.getProductId(), stockItem.getAvailableQuantity());
    }
}
