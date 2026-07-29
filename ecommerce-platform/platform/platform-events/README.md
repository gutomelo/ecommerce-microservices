# platform-events

O contrato de eventos da plataforma. **Único** lugar onde um evento de domínio é definido — nenhum microsserviço declara sua própria versão de um evento existente.

## Papel na arquitetura

Cada evento estende `BaseEvent<T>` e carrega um envelope fixo (`eventId`, `aggregateId`, `aggregateType`, `eventType`, `occurredAt`, `correlationId`, `traceId`, `version`) mais um `payload` específico. Publicador e consumidores de um evento (serviços diferentes) sempre importam a **mesma** classe deste módulo — impossível haver divergência de contrato entre quem publica e quem consome, já que ambos compilam contra o mesmo jar.

`@Jacksonized` fica só nas subclasses concretas (nunca em `BaseEvent`), porque o Jackson sempre desserializa para o tipo concreto, nunca para a base. Mesma lógica para `@JsonIgnoreProperties(ignoreUnknown = true)`: teria que estar em cada subclasse para funcionar de fato com o builder gerado pelo Lombok — herdar da base não bastava (foi descoberto testando serialização, não é intuitivo).

## API pública

Todo evento segue o mesmo padrão de fábrica: `EventoXyz.of(String aggregateId, String correlationId, String traceId, Payload payload)` — preenche `eventId` (UUID aleatório), `occurredAt` (agora), `version=1`, `eventType`/`aggregateType` (constantes) automaticamente.

| Evento | `aggregateType` | Payload | Publicado por | Consumido por |
|---|---|---|---|---|
| `OrderCreatedEvent` | `Order` | `orderId, customerId, items[], totalAmount` | `order-service` | `inventory-service`, `notification-service` |
| `StockReservedEvent` | `Stock` | `orderId, items[], totalAmount` | `inventory-service` | `payment-service`, `notification-service` |
| `StockUnavailableEvent` | `Stock` | `orderId, unavailableProductIds[], reason` | `inventory-service` | `order-service`, `notification-service` |
| `PaymentApprovedEvent` | `Payment` | `orderId, paymentId, amount, paidAt` | `payment-service` | `order-service`, `notification-service` |
| `PaymentDeclinedEvent` | `Payment` | `orderId, amount, reason` | `payment-service` | `order-service`, `notification-service` |
| `OrderConfirmedEvent` | `Order` | `orderId, customerId, confirmedAt` | `order-service` | `notification-service` |
| `OrderCancelledEvent` | `Order` | `orderId, customerId, reason, cancelledAt` | `order-service` | `inventory-service`, `notification-service` |

Catálogo completo (tópico SNS, motivo de negócio de cada evento): [`docs/events/catalogo-eventos.md`](../../docs/events/catalogo-eventos.md). Fluxo completo da Saga: [`docs/saga/fluxo-saga.md`](../../docs/saga/fluxo-saga.md).

### Por que `StockReservedEvent` carrega `totalAmount`

Decisão não óbvia, documentada no próprio código: `payment-service` consome **exclusivamente** `StockReservedEvent` (nunca `OrderCreatedEvent`) e precisa do valor do pedido para decidir aprovação/recusa. Em vez de acoplar `payment-service` a um segundo tópico só para isso, o valor é duplicado (denormalizado) no payload de `StockReservedEvent` — `inventory-service` só repassa o que já recebeu em `OrderCreatedEvent`.

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-events</artifactId>
</dependency>
```

```java
var payload = new OrderCreatedEvent.Payload(orderId, customerId, items, totalAmount);
var event = OrderCreatedEvent.of(orderId.toString(), correlationId, traceId, payload);
outboxEventStore.store(event, OrderCreatedEvent.EVENT_TYPE); // Outbox Pattern, ver platform-messaging
```

## Dependências principais

`jackson-databind` + `jackson-datatype-jsr310`, Lombok (`@SuperBuilder`, `@Jacksonized`, `@Getter`, `@EqualsAndHashCode`, `@ToString`).

## Testes

- `BaseEventTest.java` — comportamento da classe base.
- `EventCatalogSerializationTest.java` — round-trip de (de)serialização JSON de cada um dos 7 eventos do catálogo, garantindo que payloads aninhados sobrevivem intactos e que `eventType`/`aggregateType` são preenchidos corretamente pela factory `of(...)`.
