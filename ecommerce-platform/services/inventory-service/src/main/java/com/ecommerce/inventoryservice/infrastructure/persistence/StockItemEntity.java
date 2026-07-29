package com.ecommerce.inventoryservice.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * productId e a propria chave primaria (nao um id gerado localmente): o ledger de
 * estoque e indexado pelo produto que ele rastreia.
 */
@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
public class StockItemEntity {

    @Id
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;
}
