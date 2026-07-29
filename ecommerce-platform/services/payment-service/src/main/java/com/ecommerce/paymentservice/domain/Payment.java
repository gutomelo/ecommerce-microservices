package com.ecommerce.paymentservice.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root do payment-service. Regra de aprovacao simulada e deterministica
 * (ver .claude/rules/testes.md): aprova valores ate o limite configurado, recusa
 * acima dele - o limite em si e uma politica de configuracao (platform.payment.approval-threshold),
 * nao hardcoded aqui, para o dominio permanecer testavel sem depender de config.
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Payment {

    public static final String DECLINE_REASON_THRESHOLD_EXCEEDED = "valor do pedido acima do limite aprovado";

    private final UUID id;
    private final UUID orderId;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final Instant createdAt;

    public static Payment decide(UUID orderId, BigDecimal amount, BigDecimal approvalThreshold) {
        PaymentStatus status = amount.compareTo(approvalThreshold) <= 0 ? PaymentStatus.APPROVED : PaymentStatus.DECLINED;
        return Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(status)
                .build();
    }
}
