package com.ecommerce.platform.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.UUID;

/**
 * Superclasse JPA base para entidades que precisam de trilha de auditoria simples
 * (createdAt/updatedAt). Usa callbacks JPA puros para nao exigir @EnableJpaAuditing
 * em cada microsservico consumidor.
 */
@MappedSuperclass
public abstract class BaseAuditEntity extends BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BaseAuditEntity() {
    }

    protected BaseAuditEntity(UUID id) {
        super(id);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
