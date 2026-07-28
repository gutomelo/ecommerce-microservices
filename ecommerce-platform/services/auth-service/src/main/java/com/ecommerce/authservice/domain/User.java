package com.ecommerce.authservice.domain;

import com.ecommerce.platform.security.Roles;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root do auth-service. Sem qualquer anotacao de framework (Spring/JPA) -
 * a persistencia e mapeada em infrastructure/persistence (ver
 * .claude/rules/estrutura-microsservico.md).
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Roles role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Cadastro publico sempre cria um CUSTOMER. Nao ha via de API para criar ADMIN -
     * o unico admin existe via seed do Flyway (ver db/migration).
     */
    public static User register(String email, String passwordHash) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(Roles.CUSTOMER)
                .active(true)
                .build();
    }
}
