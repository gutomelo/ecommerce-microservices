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
        ProductEntity entity = product.getId() == null ? insertNew(product) : updateExisting(product);
        return toDomain(entity);
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

    private ProductEntity insertNew(Product product) {
        ProductEntity entity = new ProductEntity();
        applyFields(entity, product);
        return springDataProductRepository.save(entity);
    }

    /**
     * Busca a entidade ja gerenciada em vez de construir uma nova: uma entidade
     * transiente sem createdAt (BaseAuditEntity nao tem setter publico para esse
     * campo) sobrescreveria o valor em memoria no objeto retornado por save()
     * apos o merge, mesmo com a coluna marcada updatable=false protegendo o
     * banco - o response da API voltava com createdAt nulo depois de um update.
     */
    private ProductEntity updateExisting(Product product) {
        ProductEntity entity = springDataProductRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product not found for update: " + product.getId()));
        applyFields(entity, product);
        return springDataProductRepository.save(entity);
    }

    private void applyFields(ProductEntity entity, Product product) {
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setCategory(product.getCategory());
        entity.setPrice(product.getPrice());
        entity.setStock(product.getStock());
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
