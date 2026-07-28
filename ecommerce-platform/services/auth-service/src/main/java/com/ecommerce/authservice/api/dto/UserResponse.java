package com.ecommerce.authservice.api.dto;

import com.ecommerce.authservice.domain.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}
