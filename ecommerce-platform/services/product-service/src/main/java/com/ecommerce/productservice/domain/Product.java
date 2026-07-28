package com.ecommerce.productservice.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root do product-service. Sem anotacoes de framework - a persistencia
 * e mapeada em infrastructure/persistence (ver .claude/rules/estrutura-microsservico.md).
 * <p>
 * O estoque aqui e o catalogo simples (CRUD); a reserva/liberacao de estoque
 * durante a Saga e responsabilidade do inventory-service (ver docs/saga/fluxo-saga.md).
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Product {

    private final UUID id;
    private final String name;
    private final String description;
    private final String category;
    private final BigDecimal price;
    private final int stock;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Product create(String name, String description, String category, BigDecimal price, int stock) {
        return Product.builder()
                .name(name)
                .description(description)
                .category(category)
                .price(price)
                .stock(stock)
                .build();
    }

    public Product withUpdatedDetails(String name, String description, String category, BigDecimal price, int stock) {
        return this.toBuilder()
                .name(name)
                .description(description)
                .category(category)
                .price(price)
                .stock(stock)
                .build();
    }
}
