package com.ecommerce.customerservice.domain.port;

import com.ecommerce.customerservice.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Page<Customer> findAll(Pageable pageable);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
