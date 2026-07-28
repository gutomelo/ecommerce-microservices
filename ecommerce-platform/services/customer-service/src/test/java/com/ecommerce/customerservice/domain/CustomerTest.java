package com.ecommerce.customerservice.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void registerCreatesActiveCustomer() {
        Customer customer = Customer.register("Jane Doe", "jane@example.com", "123456");

        assertThat(customer.getName()).isEqualTo("Jane Doe");
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void withUpdatedDetailsChangesNameAndPhoneOnly() {
        Customer original = Customer.register("Jane Doe", "jane@example.com", "123456");

        Customer updated = original.withUpdatedDetails("Jane Smith", "999999");

        assertThat(updated.getName()).isEqualTo("Jane Smith");
        assertThat(updated.getPhone()).isEqualTo("999999");
        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void equalsAndHashCodeAreBasedOnId() {
        UUID id = UUID.randomUUID();
        Customer a = Customer.builder().id(id).build();
        Customer b = Customer.builder().id(id).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
