package com.ecommerce.orderservice.api;

import com.ecommerce.orderservice.api.dto.CreateOrderRequest;
import com.ecommerce.orderservice.api.dto.OrderResponse;
import com.ecommerce.orderservice.application.OrderService;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var items = request.items().stream()
                .map(item -> new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        var order = orderService.createOrder(request.customerId(), items);
        return ApiResponse.success(OrderResponse.from(order), "Pedido criado com sucesso");
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(OrderResponse.from(orderService.findById(id)));
    }
}
