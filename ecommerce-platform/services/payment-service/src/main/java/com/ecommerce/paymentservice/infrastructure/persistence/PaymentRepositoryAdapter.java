package com.ecommerce.paymentservice.infrastructure.persistence;

import com.ecommerce.paymentservice.domain.Payment;
import com.ecommerce.paymentservice.domain.port.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository springDataPaymentRepository;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository springDataPaymentRepository) {
        this.springDataPaymentRepository = springDataPaymentRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setOrderId(payment.getOrderId());
        entity.setAmount(payment.getAmount());
        entity.setStatus(payment.getStatus());
        PaymentEntity saved = springDataPaymentRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return springDataPaymentRepository.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    private Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
