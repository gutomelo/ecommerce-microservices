package com.ecommerce.paymentservice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private static final BigDecimal THRESHOLD = new BigDecimal("500.00");

    @Test
    void decideApprovesWhenAmountIsBelowThreshold() {
        Payment payment = Payment.decide(UUID.randomUUID(), new BigDecimal("100.00"), THRESHOLD);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void decideApprovesWhenAmountEqualsThreshold() {
        Payment payment = Payment.decide(UUID.randomUUID(), THRESHOLD, THRESHOLD);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void decideDeclinesWhenAmountExceedsThreshold() {
        Payment payment = Payment.decide(UUID.randomUUID(), new BigDecimal("500.01"), THRESHOLD);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void equalsAndHashCodeAreBasedOnId() {
        UUID id = UUID.randomUUID();
        Payment a = Payment.builder().id(id).build();
        Payment b = Payment.builder().id(id).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
