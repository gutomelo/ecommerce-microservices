package com.ecommerce.paymentservice.infrastructure.outbox;

import com.ecommerce.paymentservice.application.port.OutboxEventStore;
import com.ecommerce.platform.events.BaseEvent;
import com.ecommerce.platform.messaging.MessageSerializer;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxEventStoreAdapter implements OutboxEventStore {

    private final SpringDataOutboxRepository outboxRepository;
    private final MessageSerializer serializer;

    public OutboxEventStoreAdapter(SpringDataOutboxRepository outboxRepository, MessageSerializer serializer) {
        this.outboxRepository = outboxRepository;
        this.serializer = serializer;
    }

    @Override
    public void store(BaseEvent<?> event, String topic) {
        OutboxEntity entity = new OutboxEntity();
        entity.setId(event.getEventId());
        entity.setAggregateId(event.getAggregateId());
        entity.setEventType(event.getEventType());
        entity.setTopic(topic);
        entity.setPayload(serializer.serialize(event));
        entity.setStatus(OutboxStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        outboxRepository.save(entity);
    }
}
