package com.ecommerce.inventoryservice.domain;

import com.ecommerce.platform.exception.ValidationException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Ledger local de estoque do inventory-service, desacoplado do campo "stock" de
 * product-service (cada servico tem seu proprio banco - ver .claude/rules/banco-de-dados.md).
 * Produtos ainda nao vistos sao provisionados com DEFAULT_INITIAL_QUANTITY na
 * primeira tentativa de reserva (ver InventoryService).
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "productId")
public class StockItem {

    public static final int DEFAULT_INITIAL_QUANTITY = 100;

    private final UUID productId;
    private final int availableQuantity;

    public static StockItem provisionDefault(UUID productId) {
        return StockItem.builder().productId(productId).availableQuantity(DEFAULT_INITIAL_QUANTITY).build();
    }

    public boolean hasSufficientStock(int quantity) {
        return availableQuantity >= quantity;
    }

    public StockItem reserve(int quantity) {
        if (!hasSufficientStock(quantity)) {
            throw new ValidationException("Estoque insuficiente para o produto " + productId);
        }
        return this.toBuilder().availableQuantity(availableQuantity - quantity).build();
    }

    public StockItem release(int quantity) {
        return this.toBuilder().availableQuantity(availableQuantity + quantity).build();
    }
}
