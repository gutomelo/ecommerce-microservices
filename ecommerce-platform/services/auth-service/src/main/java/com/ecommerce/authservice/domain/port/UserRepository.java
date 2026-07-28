package com.ecommerce.authservice.domain.port;

import com.ecommerce.authservice.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistencia do dominio, implementada em infrastructure/persistence.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);
}
