package com.ecommerce.paymentservice.application;

import com.ecommerce.paymentservice.application.port.OutboxEventStore;
import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.port.PaymentRepository;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private OutboxEventStore outboxEventStore;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        outboxEventStore = mock(OutboxEventStore.class);
        PaymentProperties properties = new PaymentProperties();
        properties.setApprovalThreshold(new BigDecimal("500.00"));
        paymentService = new PaymentService(paymentRepository, outboxEventStore, properties);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment argument = inv.getArgument(0);
            return argument.toBuilder().id(UUID.randomUUID()).build();
        });
    }

    @Test
    void processPaymentPublishesPaymentApprovedWhenBelowThreshold() {
        UUID orderId = UUID.randomUUID();

        paymentService.processPayment(orderId, new BigDecimal("100.00"), "corr-1", "trace-1");

        verify(outboxEventStore).store(any(PaymentApprovedEvent.class), eq(PaymentApprovedEvent.EVENT_TYPE));
        verify(outboxEventStore, never()).store(any(PaymentDeclinedEvent.class), any());
    }

    @Test
    void processPaymentPublishesPaymentDeclinedWhenAboveThreshold() {
        UUID orderId = UUID.randomUUID();

        paymentService.processPayment(orderId, new BigDecimal("999.99"), "corr-1", "trace-1");

        verify(outboxEventStore).store(any(PaymentDeclinedEvent.class), eq(PaymentDeclinedEvent.EVENT_TYPE));
        verify(outboxEventStore, never()).store(any(PaymentApprovedEvent.class), any());
    }

    @Test
    void findByOrderIdDelegatesToRepository() {
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder().id(UUID.randomUUID()).orderId(orderId).build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of(payment));

        var result = paymentService.findByOrderId(orderId);

        org.assertj.core.api.Assertions.assertThat(result).containsExactly(payment);
    }
}
