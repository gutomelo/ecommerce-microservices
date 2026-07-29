package com.ecommerce.notificationservice.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
