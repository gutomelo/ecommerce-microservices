package com.ecommerce.customerservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    boolean existsByEmail(String email);
}
