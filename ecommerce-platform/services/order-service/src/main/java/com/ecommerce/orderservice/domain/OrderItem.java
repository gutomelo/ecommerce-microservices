package com.ecommerce.orderservice.domain;

import com.ecommerce.platform.exception.ValidationException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Value object - item de um pedido. Imutavel, sem identidade propria.
 */
public record OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (quantity <= 0) {
            throw new ValidationException("A quantidade do item deve ser maior que zero");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new ValidationException("O preco unitario do item nao pode ser negativo");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
