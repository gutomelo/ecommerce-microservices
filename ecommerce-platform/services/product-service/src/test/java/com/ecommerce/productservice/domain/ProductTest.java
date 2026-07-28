package com.ecommerce.productservice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void createBuildsProductWithGivenFields() {
        Product product = Product.create("Widget", "A useful widget", "tools", new BigDecimal("19.90"), 10);

        assertThat(product.getName()).isEqualTo("Widget");
        assertThat(product.getCategory()).isEqualTo("tools");
        assertThat(product.getPrice()).isEqualByComparingTo("19.90");
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    void withUpdatedDetailsReplacesAllMutableFields() {
        Product original = Product.create("Widget", "desc", "tools", new BigDecimal("19.90"), 10);

        Product updated = original.withUpdatedDetails("Gadget", "new desc", "electronics", new BigDecimal("29.90"), 5);

        assertThat(updated.getName()).isEqualTo("Gadget");
        assertThat(updated.getDescription()).isEqualTo("new desc");
        assertThat(updated.getCategory()).isEqualTo("electronics");
        assertThat(updated.getPrice()).isEqualByComparingTo("29.90");
        assertThat(updated.getStock()).isEqualTo(5);
    }

    @Test
    void equalsAndHashCodeAreBasedOnId() {
        UUID id = UUID.randomUUID();
        Product a = Product.builder().id(id).build();
        Product b = Product.builder().id(id).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
