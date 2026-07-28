package com.ecommerce.orderservice.infrastructure.persistence;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.port.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springDataOrderRepository;

    public OrderRepositoryAdapter(SpringDataOrderRepository springDataOrderRepository) {
        this.springDataOrderRepository = springDataOrderRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = order.getId() == null ? insertNew(order) : updateStatus(order);
        return toDomain(entity);
    }

    /**
     * @Transactional aqui (nao so no caller) e obrigatorio: SimpleJpaRepository.findById()
     * fecha a sessao Hibernate ao retornar, entao o acesso lazy a `items` dentro de
     * toDomain() precisa acontecer na MESMA transacao da leitura, nao depois dela.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return springDataOrderRepository.findById(id).map(this::toDomain);
    }

    private OrderEntity insertNew(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(order.getCustomerId());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setStatus(order.getStatus());
        order.getItems().forEach(item -> entity.getItems().add(toItemEntity(item, entity)));
        return springDataOrderRepository.save(entity);
    }

    /**
     * Itens sao imutaveis apos a criacao do pedido - so o status muda em
     * confirm()/cancel(). Atualiza a entidade ja gerenciada em vez de reconstruir
     * o grafo completo a partir do dominio, o que evitaria remover/reinserir os
     * order_items a cada transicao de status (cascade ALL + orphanRemoval).
     */
    private OrderEntity updateStatus(Order order) {
        OrderEntity entity = springDataOrderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalStateException("Order not found for update: " + order.getId()));
        entity.setStatus(order.getStatus());
        return springDataOrderRepository.save(entity);
    }

    private OrderItemEntity toItemEntity(OrderItem item, OrderEntity order) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setOrder(order);
        entity.setProductId(item.productId());
        entity.setQuantity(item.quantity());
        entity.setUnitPrice(item.unitPrice());
        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return Order.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .items(items)
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
