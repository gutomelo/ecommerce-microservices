package com.ecommerce.orderservice.application;

import com.ecommerce.orderservice.application.port.OutboxEventStore;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import com.ecommerce.platform.events.BaseEvent;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OutboxEventStore outboxEventStore;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        outboxEventStore = mock(OutboxEventStore.class);
        orderService = new OrderService(orderRepository, outboxEventStore);
        // Simula o comportamento real de OrderRepositoryAdapter: o id e gerado
        // pelo banco na primeira insercao (Order.create() nao tem id ainda).
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order argument = inv.getArgument(0);
            return argument.getId() != null ? argument : argument.toBuilder().id(UUID.randomUUID()).build();
        });
    }

    private List<OrderItem> sampleItems() {
        return List.of(new OrderItem(UUID.randomUUID(), 2, new BigDecimal("19.90")));
    }

    @Test
    void createOrderPersistsOrderAndStoresOrderCreatedEventInOutbox() {
        UUID customerId = UUID.randomUUID();

        Order created = orderService.createOrder(customerId, sampleItems());

        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getCustomerId()).isEqualTo(customerId);
        verify(outboxEventStore).store(any(OrderCreatedEvent.class), eq(OrderCreatedEvent.EVENT_TYPE));
    }

    @Test
    void findByIdThrowsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmOrderTransitionsToConfirmedAndStoresOrderConfirmedEvent() {
        Order pending = Order.create(UUID.randomUUID(), sampleItems()).toBuilder().id(UUID.randomUUID()).build();
        when(orderRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        orderService.confirmOrder(pending.getId(), "corr-1", "trace-1");

        verify(orderRepository).save(argThatStatus(OrderStatus.CONFIRMED));
        verify(outboxEventStore).store(any(OrderConfirmedEvent.class), eq(OrderConfirmedEvent.EVENT_TYPE));
    }

    @Test
    void confirmOrderIsNoOpWhenOrderIsNotPending() {
        Order alreadyConfirmed = Order.create(UUID.randomUUID(), sampleItems())
                .toBuilder().id(UUID.randomUUID()).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findById(alreadyConfirmed.getId())).thenReturn(Optional.of(alreadyConfirmed));

        orderService.confirmOrder(alreadyConfirmed.getId(), "corr-1", "trace-1");

        verify(orderRepository, never()).save(any());
        verify(outboxEventStore, never()).store(any(BaseEvent.class), any());
    }

    @Test
    void cancelOrderTransitionsToCancelledAndStoresOrderCancelledEvent() {
        Order pending = Order.create(UUID.randomUUID(), sampleItems()).toBuilder().id(UUID.randomUUID()).build();
        when(orderRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        orderService.cancelOrder(pending.getId(), "estoque insuficiente", "corr-1", "trace-1");

        verify(orderRepository).save(argThatStatus(OrderStatus.CANCELLED));
        verify(outboxEventStore).store(any(OrderCancelledEvent.class), eq(OrderCancelledEvent.EVENT_TYPE));
    }

    @Test
    void cancelOrderIsNoOpWhenOrderIsNotPending() {
        Order alreadyCancelled = Order.create(UUID.randomUUID(), sampleItems())
                .toBuilder().id(UUID.randomUUID()).status(OrderStatus.CANCELLED).build();
        when(orderRepository.findById(alreadyCancelled.getId())).thenReturn(Optional.of(alreadyCancelled));

        orderService.cancelOrder(alreadyCancelled.getId(), "pagamento recusado", "corr-1", "trace-1");

        verify(orderRepository, never()).save(any());
        verify(outboxEventStore, never()).store(any(BaseEvent.class), any());
    }

    private Order argThatStatus(OrderStatus status) {
        return org.mockito.ArgumentMatchers.argThat(order -> order != null && order.getStatus() == status);
    }
}
