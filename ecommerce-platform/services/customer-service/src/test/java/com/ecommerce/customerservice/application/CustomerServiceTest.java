package com.ecommerce.customerservice.application;

import com.ecommerce.customerservice.domain.Customer;
import com.ecommerce.customerservice.domain.port.CustomerRepository;
import com.ecommerce.platform.exception.ConflictException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        service = new CustomerService(customerRepository);
    }

    private Customer sampleCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("123456")
                .active(true)
                .build();
    }

    @Test
    void createsCustomerWhenEmailNotTaken() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.create("Jane Doe", "jane@example.com", "123456");

        assertThat(result.getName()).isEqualTo("Jane Doe");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void throwsConflictWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Jane Doe", "jane@example.com", "123456"))
                .isInstanceOf(ConflictException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsCustomer() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThat(service.findById(customer.getId())).isEqualTo(customer);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllDelegatesToRepository() {
        var page = new PageImpl<>(List.of(sampleCustomer()), PageRequest.of(0, 10), 1);
        when(customerRepository.findAll(any())).thenReturn(page);

        var result = service.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updatesNameAndPhone() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer updated = service.update(customer.getId(), "New Name", "999999");

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getPhone()).isEqualTo("999999");
        assertThat(updated.getEmail()).isEqualTo(customer.getEmail());
    }

    @Test
    void deleteRemovesExistingCustomer() {
        Customer customer = sampleCustomer();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        service.delete(customer.getId());

        verify(customerRepository).deleteById(customer.getId());
    }

    @Test
    void deleteThrowsWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(customerRepository, never()).deleteById(any());
    }
}
