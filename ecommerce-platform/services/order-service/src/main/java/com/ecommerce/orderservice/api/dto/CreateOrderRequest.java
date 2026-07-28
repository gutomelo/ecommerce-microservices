package com.ecommerce.orderservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(@NotNull UUID customerId, @NotEmpty @Valid List<Item> items) {

    public record Item(@NotNull UUID productId, @Positive int quantity, @NotNull @PositiveOrZero BigDecimal unitPrice) {
    }
}
