package com.ecommerce.inventoryservice.domain;

import com.ecommerce.platform.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemTest {

    @Test
    void provisionDefaultStartsWithDefaultQuantity() {
        StockItem item = StockItem.provisionDefault(UUID.randomUUID());

        assertThat(item.getAvailableQuantity()).isEqualTo(StockItem.DEFAULT_INITIAL_QUANTITY);
    }

    @Test
    void hasSufficientStockComparesAgainstAvailableQuantity() {
        StockItem item = StockItem.builder().productId(UUID.randomUUID()).availableQuantity(5).build();

        assertThat(item.hasSufficientStock(5)).isTrue();
        assertThat(item.hasSufficientStock(6)).isFalse();
    }

    @Test
    void reserveDecrementsAvailableQuantity() {
        StockItem item = StockItem.builder().productId(UUID.randomUUID()).availableQuantity(10).build();

        StockItem reserved = item.reserve(3);

        assertThat(reserved.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void reserveThrowsWhenInsufficientStock() {
        StockItem item = StockItem.builder().productId(UUID.randomUUID()).availableQuantity(2).build();

        assertThatThrownBy(() -> item.reserve(3)).isInstanceOf(ValidationException.class);
    }

    @Test
    void releaseIncrementsAvailableQuantity() {
        StockItem item = StockItem.builder().productId(UUID.randomUUID()).availableQuantity(5).build();

        StockItem released = item.release(4);

        assertThat(released.getAvailableQuantity()).isEqualTo(9);
    }

    @Test
    void equalsAndHashCodeAreBasedOnProductId() {
        UUID productId = UUID.randomUUID();
        StockItem a = StockItem.builder().productId(productId).availableQuantity(1).build();
        StockItem b = StockItem.builder().productId(productId).availableQuantity(99).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
