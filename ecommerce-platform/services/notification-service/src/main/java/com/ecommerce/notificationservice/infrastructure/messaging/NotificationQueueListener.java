package com.ecommerce.notificationservice.infrastructure.messaging;

import com.ecommerce.platform.messaging.EventTypeRouter;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

/**
 * Unico ponto de entrada da fila notification-queue, que recebe os 7 tipos de
 * evento do catalogo (assina todos os topicos - ver
 * infrastructure/localstack/init/init-aws.sh). Delega para o EventTypeRouter,
 * que roteia por eventType para o IdempotentEventDispatcher correto (ver
 * com.ecommerce.notificationservice.config.NotificationQueueRoutingConfig).
 */
@Component
public class NotificationQueueListener {

    private final EventTypeRouter eventTypeRouter;

    public NotificationQueueListener(EventTypeRouter eventTypeRouter) {
        this.eventTypeRouter = eventTypeRouter;
    }

    @SqsListener("notification-queue")
    public void listen(String rawMessageBody) {
        eventTypeRouter.route(rawMessageBody);
    }
}
