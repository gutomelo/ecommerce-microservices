package com.ecommerce.productservice.infrastructure.persistence;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.domain.port.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final SpringDataProductRepository springDataProductRepository;

    public ProductRepositoryAdapter(SpringDataProductRepository springDataProductRepository) {
        this.springDataProductRepository = springDataProductRepository;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        ProductEntity saved = springDataProductRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return springDataProductRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return springDataProductRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        springDataProductRepository.deleteById(id);
    }

    private ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setCategory(product.getCategory());
        entity.setPrice(product.getPrice());
        entity.setStock(product.getStock());
        return entity;
    }

    private Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
