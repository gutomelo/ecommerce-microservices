package com.ecommerce.paymentservice.domain.port;

import com.ecommerce.paymentservice.domain.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    List<Payment> findByOrderId(UUID orderId);
}
