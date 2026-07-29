package com.ecommerce.customerservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRequest(
        @NotBlank String name,
        String phone) {
}
