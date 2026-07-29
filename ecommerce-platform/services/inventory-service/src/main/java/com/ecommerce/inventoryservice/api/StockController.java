package com.ecommerce.inventoryservice.api;

import com.ecommerce.inventoryservice.api.dto.StockItemResponse;
import com.ecommerce.inventoryservice.application.InventoryService;
import com.ecommerce.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Somente leitura: mutacoes de estoque acontecem exclusivamente via eventos
 * consumidos da Saga (OrderCreated/OrderCancelled), nunca via API.
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    private final InventoryService inventoryService;

    public StockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ApiResponse<StockItemResponse> findByProductId(@PathVariable UUID productId) {
        return ApiResponse.success(StockItemResponse.from(inventoryService.getStockLevel(productId)));
    }
}
