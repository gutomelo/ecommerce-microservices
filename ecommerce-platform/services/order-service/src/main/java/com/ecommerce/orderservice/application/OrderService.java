package com.ecommerce.orderservice.application;

import com.ecommerce.orderservice.application.port.OutboxEventStore;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.platform.common.constants.CorrelationHeaders;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventStore outboxEventStore;

    public OrderService(OrderRepository orderRepository, OutboxEventStore outboxEventStore) {
        this.orderRepository = orderRepository;
        this.outboxEventStore = outboxEventStore;
    }

    @Transactional
    public Order createOrder(UUID customerId, List<OrderItem> items) {
        Order order = orderRepository.save(Order.create(customerId, items));

        var payload = new OrderCreatedEvent.Payload(
                order.getId(), order.getCustomerId(), toEventItems(order.getItems()), order.getTotalAmount());
        var event = OrderCreatedEvent.of(order.getId().toString(), correlationIdFromContext(), traceIdFromContext(), payload);
        outboxEventStore.store(event, OrderCreatedEvent.EVENT_TYPE);

        return order;
    }

    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Order", id));
    }

    /**
     * Reage a PaymentApprovedEvent. O guard de status (em vez de deixar
     * Order.confirm() lancar) e uma segunda camada de idempotencia: alem do
     * eventId (ja coberto por IdempotentEventDispatcher), protege contra
     * mensagens de tipos diferentes que tentem fechar a Saga do mesmo pedido
     * mais de uma vez (ex.: redelivery cruzado com uma corrida de eventos).
     */
    @Transactional
    public void confirmOrder(UUID orderId, String correlationId, String traceId) {
        Order order = findById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        Order confirmed = orderRepository.save(order.confirm());

        var payload = new OrderConfirmedEvent.Payload(confirmed.getId(), confirmed.getCustomerId(), Instant.now());
        var event = OrderConfirmedEvent.of(confirmed.getId().toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, OrderConfirmedEvent.EVENT_TYPE);
    }

    /**
     * Reage a PaymentDeclinedEvent/StockUnavailableEvent (compensacao). Mesmo
     * guard de idempotencia de dominio descrito em {@link #confirmOrder}.
     */
    @Transactional
    public void cancelOrder(UUID orderId, String reason, String correlationId, String traceId) {
        Order order = findById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        Order cancelled = orderRepository.save(order.cancel());

        var payload = new OrderCancelledEvent.Payload(cancelled.getId(), cancelled.getCustomerId(), reason, Instant.now());
        var event = OrderCancelledEvent.of(cancelled.getId().toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, OrderCancelledEvent.EVENT_TYPE);
    }

    private List<OrderCreatedEvent.Payload.Item> toEventItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderCreatedEvent.Payload.Item(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
    }

    private String correlationIdFromContext() {
        String existing = MDC.get(CorrelationHeaders.CORRELATION_ID_MDC_KEY);
        return existing != null ? existing : UUID.randomUUID().toString();
    }

    private String traceIdFromContext() {
        String existing = MDC.get(CorrelationHeaders.TRACE_ID_MDC_KEY);
        return existing != null ? existing : UUID.randomUUID().toString();
    }
}
