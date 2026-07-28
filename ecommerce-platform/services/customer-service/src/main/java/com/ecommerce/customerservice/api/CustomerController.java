package com.ecommerce.customerservice.api;

import com.ecommerce.customerservice.api.dto.CreateCustomerRequest;
import com.ecommerce.customerservice.api.dto.CustomerResponse;
import com.ecommerce.customerservice.api.dto.UpdateCustomerRequest;
import com.ecommerce.customerservice.application.CustomerService;
import com.ecommerce.platform.common.ApiResponse;
import com.ecommerce.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        var customer = customerService.create(request.name(), request.email(), request.phone());
        return ApiResponse.success(CustomerResponse.from(customer), "Cliente cadastrado com sucesso");
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(CustomerResponse.from(customerService.findById(id)));
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> findAll(Pageable pageable) {
        var page = customerService.findAll(pageable).map(CustomerResponse::from);
        return ApiResponse.success(PageResponse.from(page));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        var customer = customerService.update(id, request.name(), request.phone());
        return ApiResponse.success(CustomerResponse.from(customer), "Cliente atualizado com sucesso");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customerService.delete(id);
    }
}
