package com.ecommerce.customerservice.application;

import com.ecommerce.customerservice.domain.Customer;
import com.ecommerce.customerservice.domain.port.CustomerRepository;
import com.ecommerce.platform.exception.ConflictException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(String name, String email, String phone) {
        if (customerRepository.existsByEmail(email)) {
            throw new ConflictException("Ja existe um cliente cadastrado com este e-mail");
        }
        return customerRepository.save(Customer.register(name, email, phone));
    }

    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Customer", id));
    }

    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Customer update(UUID id, String name, String phone) {
        Customer existing = findById(id);
        return customerRepository.save(existing.withUpdatedDetails(name, phone));
    }

    public void delete(UUID id) {
        findById(id);
        customerRepository.deleteById(id);
    }
}
