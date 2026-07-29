package com.ecommerce.productservice.application;

import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.domain.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        service = new ProductService(productRepository);
    }

    private Product sampleProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .name("Widget")
                .description("desc")
                .category("tools")
                .price(new BigDecimal("19.90"))
                .stock(10)
                .build();
    }

    @Test
    void createsProduct() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.create("Widget", "desc", "tools", new BigDecimal("19.90"), 10);

        assertThat(result.getName()).isEqualTo("Widget");
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    void findByIdReturnsProduct() {
        Product product = sampleProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(service.findById(product.getId())).isEqualTo(product);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllDelegatesToRepository() {
        var page = new PageImpl<>(List.of(sampleProduct()), PageRequest.of(0, 10), 1);
        when(productRepository.findAll(any())).thenReturn(page);

        assertThat(service.findAll(PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void updatesAllMutableFields() {
        Product product = sampleProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.update(product.getId(), "Gadget", "new desc", "electronics",
                new BigDecimal("29.90"), 5);

        assertThat(updated.getName()).isEqualTo("Gadget");
        assertThat(updated.getStock()).isEqualTo(5);
    }

    @Test
    void deleteRemovesExistingProduct() {
        Product product = sampleProduct();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        service.delete(product.getId());

        verify(productRepository).deleteById(product.getId());
    }

    @Test
    void deleteThrowsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }
}
