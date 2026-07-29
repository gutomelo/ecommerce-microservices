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
        CustomerEntity entity = customer.getId() == null ? insertNew(customer) : updateExisting(customer);
        return toDomain(entity);
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

    private CustomerEntity insertNew(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        applyFields(entity, customer);
        return springDataCustomerRepository.save(entity);
    }

    /**
     * Busca a entidade ja gerenciada em vez de construir uma nova: uma entidade
     * transiente sem createdAt (BaseAuditEntity nao tem setter publico para esse
     * campo) sobrescreveria o valor em memoria no objeto retornado por save()
     * apos o merge, mesmo com a coluna marcada updatable=false protegendo o
     * banco - o response da API voltava com createdAt nulo depois de um update.
     */
    private CustomerEntity updateExisting(Customer customer) {
        CustomerEntity entity = springDataCustomerRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Customer not found for update: " + customer.getId()));
        applyFields(entity, customer);
        return springDataCustomerRepository.save(entity);
    }

    private void applyFields(CustomerEntity entity, Customer customer) {
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setPhone(customer.getPhone());
        entity.setActive(customer.isActive());
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
