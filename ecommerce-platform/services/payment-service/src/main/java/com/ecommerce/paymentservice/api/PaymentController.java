package com.ecommerce.paymentservice.api;

import com.ecommerce.paymentservice.api.dto.PaymentResponse;
import com.ecommerce.paymentservice.application.PaymentService;
import com.ecommerce.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Somente leitura: pagamentos so sao criados via evento consumido (StockReserved),
 * nunca via API.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<PaymentResponse>> findByOrderId(@PathVariable UUID orderId) {
        var payments = paymentService.findByOrderId(orderId).stream().map(PaymentResponse::from).toList();
        return ApiResponse.success(payments);
    }
}
