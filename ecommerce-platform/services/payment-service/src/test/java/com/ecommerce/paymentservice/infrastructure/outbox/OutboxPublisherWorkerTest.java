package com.ecommerce.paymentservice.infrastructure.outbox;

import com.ecommerce.platform.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherWorkerTest {

    private SpringDataOutboxRepository outboxRepository;
    private EventPublisher eventPublisher;
    private OutboxPublisherWorker worker;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(SpringDataOutboxRepository.class);
        eventPublisher = mock(EventPublisher.class);
        worker = new OutboxPublisherWorker(outboxRepository, eventPublisher);
    }

    private OutboxEntity pendingEntry() {
        OutboxEntity entity = new OutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateId(UUID.randomUUID().toString());
        entity.setEventType("PaymentApproved");
        entity.setTopic("PaymentApproved");
        entity.setPayload("{}");
        entity.setStatus(OutboxStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    @Test
    void publishesPendingEntriesAndMarksAsPublished() {
        OutboxEntity entry = pendingEntry();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(entry));

        worker.publishPendingEvents();

        verify(eventPublisher).publishRaw(entry.getTopic(), entry.getEventType(), entry.getPayload());
        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(entry.getPublishedAt()).isNotNull();
        verify(outboxRepository).save(entry);
    }

    @Test
    void leavesEntryPendingWhenPublishFails() {
        OutboxEntity entry = pendingEntry();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(entry));
        doThrow(new RuntimeException("SNS indisponivel")).when(eventPublisher)
                .publishRaw(anyString(), anyString(), anyString());

        worker.publishPendingEvents();

        assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxRepository, never()).save(any());
    }
}
