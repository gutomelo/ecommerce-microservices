package com.ecommerce.paymentservice.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Limite de aprovacao automatica do pagamento simulado (ver Payment.decide()).
 * Configuravel para permitir cenarios deterministicos de teste (aprovado vs.
 * recusado) sem depender de um valor hardcoded no dominio.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.payment")
public class PaymentProperties {

    private BigDecimal approvalThreshold = new BigDecimal("500.00");
}
