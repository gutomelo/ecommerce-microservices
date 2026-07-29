package com.ecommerce.orderservice.domain;

import com.ecommerce.platform.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private List<OrderItem> sampleItems() {
        return List.of(
                new OrderItem(UUID.randomUUID(), 2, new BigDecimal("19.90")),
                new OrderItem(UUID.randomUUID(), 1, new BigDecimal("9.90")));
    }

    @Test
    void createComputesTotalAmountAsPending() {
        Order order = Order.create(UUID.randomUUID(), sampleItems());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("49.70");
    }

    @Test
    void createRejectsEmptyItems() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsNullItems() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void confirmTransitionsPendingToConfirmed() {
        Order order = Order.create(UUID.randomUUID(), sampleItems());

        Order confirmed = order.confirm();

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelTransitionsPendingToCancelled() {
        Order order = Order.create(UUID.randomUUID(), sampleItems());

        Order cancelled = order.cancel();

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cannotConfirmAlreadyConfirmedOrder() {
        Order confirmed = Order.create(UUID.randomUUID(), sampleItems()).confirm();

        assertThatThrownBy(confirmed::confirm).isInstanceOf(ValidationException.class);
    }

    @Test
    void cannotCancelAlreadyCancelledOrder() {
        Order cancelled = Order.create(UUID.randomUUID(), sampleItems()).cancel();

        assertThatThrownBy(cancelled::cancel).isInstanceOf(ValidationException.class);
    }

    @Test
    void equalsAndHashCodeAreBasedOnId() {
        UUID id = UUID.randomUUID();
        Order a = Order.builder().id(id).build();
        Order b = Order.builder().id(id).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
