package com.ecommerce.productservice.application;

import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.domain.port.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(String name, String description, String category, BigDecimal price, int stock) {
        return productRepository.save(Product.create(name, description, category, price, stock));
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Product", id));
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product update(UUID id, String name, String description, String category, BigDecimal price, int stock) {
        Product existing = findById(id);
        return productRepository.save(existing.withUpdatedDetails(name, description, category, price, stock));
    }

    public void delete(UUID id) {
        findById(id);
        productRepository.deleteById(id);
    }
}
