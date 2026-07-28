package com.ecommerce.customerservice.infrastructure.persistence;

import com.ecommerce.customerservice.domain.Customer;
import com.ecommerce.customerservice.domain.port.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final SpringDataCustomerRepository springDataCustomerRepository;

    public CustomerRepositoryAdapter(SpringDataCustomerRepository springDataCustomerRepository) {
        this.springDataCustomerRepository = springDataCustomerRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = toEntity(customer);
        CustomerEntity saved = springDataCustomerRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return springDataCustomerRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Customer> findAll(Pageable pageable) {
        return springDataCustomerRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataCustomerRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        springDataCustomerRepository.deleteById(id);
    }

    private CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId());
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setPhone(customer.getPhone());
        entity.setActive(customer.isActive());
        return entity;
    }

    private Customer toDomain(CustomerEntity entity) {
        return Customer.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
