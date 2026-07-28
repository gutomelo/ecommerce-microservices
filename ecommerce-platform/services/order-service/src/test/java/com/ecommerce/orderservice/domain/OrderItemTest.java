package com.ecommerce.orderservice.domain;

import com.ecommerce.platform.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    void subtotalMultipliesQuantityByUnitPrice() {
        OrderItem item = new OrderItem(UUID.randomUUID(), 3, new BigDecimal("19.90"));

        assertThat(item.subtotal()).isEqualByComparingTo("59.70");
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), 0, BigDecimal.TEN))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNegativeUnitPrice() {
        assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), 1, new BigDecimal("-1")))
                .isInstanceOf(ValidationException.class);
    }
}
