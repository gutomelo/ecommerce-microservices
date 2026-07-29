package com.ecommerce.orderservice.infrastructure.messaging;

import com.ecommerce.platform.messaging.EventTypeRouter;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

/**
 * Unico ponto de entrada da fila order-queue, que recebe StockUnavailable,
 * PaymentApproved e PaymentDeclined (fan-out de multiplos topicos SNS - ver
 * infrastructure/localstack/init/init-aws.sh). Delega para o EventTypeRouter, que
 * roteia por eventType para o IdempotentEventDispatcher correto (ver
 * com.ecommerce.orderservice.config.OrderQueueRoutingConfig). Se dispatch()
 * lancar, a mensagem nao e confirmada (ack) e volta para a fila, seguindo o fluxo
 * normal de retry/DLQ (ver .claude/rules/resiliencia.md).
 */
@Component
public class OrderQueueListener {

    private final EventTypeRouter eventTypeRouter;

    public OrderQueueListener(EventTypeRouter eventTypeRouter) {
        this.eventTypeRouter = eventTypeRouter;
    }

    @SqsListener("order-queue")
    public void listen(String rawMessageBody) {
        eventTypeRouter.route(rawMessageBody);
    }
}
