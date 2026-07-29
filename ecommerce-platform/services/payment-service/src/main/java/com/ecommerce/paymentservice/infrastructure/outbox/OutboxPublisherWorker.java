package com.ecommerce.paymentservice.infrastructure.outbox;

import com.ecommerce.platform.messaging.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publica de forma assincrona as linhas PENDING da tabela outbox (Outbox Pattern,
 * ver .claude/rules/comunicacao-eventos.md). Cada linha e publicada e marcada
 * individualmente para que a falha de uma nao bloqueie as demais.
 */
@Component
public class OutboxPublisherWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherWorker.class);

    private final SpringDataOutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;

    public OutboxPublisherWorker(SpringDataOutboxRepository outboxRepository, EventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.publish-interval-millis:2000}")
    public void publishPendingEvents() {
        List<OutboxEntity> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEntity entry : pending) {
            publish(entry);
        }
    }

    @Transactional
    void publish(OutboxEntity entry) {
        try {
            eventPublisher.publishRaw(entry.getTopic(), entry.getEventType(), entry.getPayload());
            entry.setStatus(OutboxStatus.PUBLISHED);
            entry.setPublishedAt(Instant.now());
            outboxRepository.save(entry);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento do outbox (id={}, eventType={}) - sera tentado novamente",
                    entry.getId(), entry.getEventType(), e);
        }
    }
}
