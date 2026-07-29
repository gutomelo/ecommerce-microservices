package com.ecommerce.paymentservice.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
