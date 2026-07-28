package com.ecommerce.productservice.domain.port;

import com.ecommerce.productservice.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Page<Product> findAll(Pageable pageable);

    void deleteById(UUID id);
}
