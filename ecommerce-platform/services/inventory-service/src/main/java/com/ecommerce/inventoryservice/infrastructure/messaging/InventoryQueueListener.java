package com.ecommerce.inventoryservice.infrastructure.messaging;

import com.ecommerce.platform.messaging.EventTypeRouter;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

/**
 * Unico ponto de entrada da fila inventory-queue, que recebe OrderCreated e
 * OrderCancelled (fan-out de multiplos topicos SNS - ver
 * infrastructure/localstack/init/init-aws.sh). Delega para o EventTypeRouter, que
 * roteia por eventType para o IdempotentEventDispatcher correto (ver
 * com.ecommerce.inventoryservice.config.InventoryQueueRoutingConfig).
 */
@Component
public class InventoryQueueListener {

    private final EventTypeRouter eventTypeRouter;

    public InventoryQueueListener(EventTypeRouter eventTypeRouter) {
        this.eventTypeRouter = eventTypeRouter;
    }

    @SqsListener("inventory-queue")
    public void listen(String rawMessageBody) {
        eventTypeRouter.route(rawMessageBody);
    }
}
