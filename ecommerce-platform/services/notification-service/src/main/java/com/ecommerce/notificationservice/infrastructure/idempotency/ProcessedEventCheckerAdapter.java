package com.ecommerce.notificationservice.infrastructure.idempotency;

import com.ecommerce.platform.messaging.ProcessedEventChecker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProcessedEventCheckerAdapter implements ProcessedEventChecker {

    private final SpringDataProcessedEventRepository repository;

    public ProcessedEventCheckerAdapter(SpringDataProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    @Transactional
    public void markProcessed(UUID eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setProcessedAt(Instant.now());
        repository.save(entity);
    }
}
