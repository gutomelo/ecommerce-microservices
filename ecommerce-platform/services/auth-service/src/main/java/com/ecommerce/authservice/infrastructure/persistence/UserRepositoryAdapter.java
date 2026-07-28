package com.ecommerce.authservice.infrastructure.persistence;

import com.ecommerce.authservice.domain.User;
import com.ecommerce.authservice.domain.port.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = springDataUserRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole());
        entity.setActive(user.isActive());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
