package com.ecommerce.customerservice.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root do customer-service. Sem anotacoes de framework - a persistencia
 * e mapeada em infrastructure/persistence (ver .claude/rules/estrutura-microsservico.md).
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Customer {

    private final UUID id;
    private final String name;
    private final String email;
    private final String phone;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Customer register(String name, String email, String phone) {
        return Customer.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .active(true)
                .build();
    }

    public Customer withUpdatedDetails(String name, String phone) {
        return this.toBuilder()
                .name(name)
                .phone(phone)
                .build();
    }
}
