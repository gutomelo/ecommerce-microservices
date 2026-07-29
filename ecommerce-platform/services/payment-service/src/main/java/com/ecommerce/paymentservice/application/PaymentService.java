package com.ecommerce.paymentservice.application;

import com.ecommerce.paymentservice.application.port.OutboxEventStore;
import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.PaymentStatus;
import com.ecommerce.paymentservice.domain.port.PaymentRepository;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventStore outboxEventStore;
    private final PaymentProperties paymentProperties;

    public PaymentService(PaymentRepository paymentRepository, OutboxEventStore outboxEventStore,
                           PaymentProperties paymentProperties) {
        this.paymentRepository = paymentRepository;
        this.outboxEventStore = outboxEventStore;
        this.paymentProperties = paymentProperties;
    }

    @Transactional
    public void processPayment(UUID orderId, BigDecimal amount, String correlationId, String traceId) {
        Payment payment = paymentRepository.save(
                Payment.decide(orderId, amount, paymentProperties.getApprovalThreshold()));

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            publishPaymentApproved(payment, correlationId, traceId);
        } else {
            publishPaymentDeclined(payment, correlationId, traceId);
        }
    }

    public List<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private void publishPaymentApproved(Payment payment, String correlationId, String traceId) {
        var payload = new PaymentApprovedEvent.Payload(payment.getOrderId(), payment.getId(), payment.getAmount(), Instant.now());
        var event = PaymentApprovedEvent.of(payment.getOrderId().toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, PaymentApprovedEvent.EVENT_TYPE);
    }

    private void publishPaymentDeclined(Payment payment, String correlationId, String traceId) {
        var payload = new PaymentDeclinedEvent.Payload(
                payment.getOrderId(), payment.getAmount(), Payment.DECLINE_REASON_THRESHOLD_EXCEEDED);
        var event = PaymentDeclinedEvent.of(payment.getOrderId().toString(), correlationId, traceId, payload);
        outboxEventStore.store(event, PaymentDeclinedEvent.EVENT_TYPE);
    }
}
