# Fluxo da Saga (Coreografia)

Nenhum serviço chama outro diretamente. Toda a coordenação acontece através da publicação e do consumo de eventos via SNS/SQS. O `correlationId` é gerado na criação do pedido e propagado em todos os eventos subsequentes da mesma transação distribuída.

## Fluxo de sucesso

```mermaid
sequenceDiagram
    participant Cliente
    participant Order as order-service
    participant Inventory as inventory-service
    participant Payment as payment-service
    participant Notification as notification-service

    Cliente->>Order: Cria pedido (PENDING)
    Order-->>Order: Outbox: OrderCreated
    Order->>Inventory: (SNS/SQS) OrderCreated
    Inventory-->>Inventory: Reserva estoque
    Inventory->>Payment: (SNS/SQS) StockReserved
    Payment-->>Payment: Processa pagamento
    Payment->>Order: (SNS/SQS) PaymentApproved
    Order-->>Order: Status = CONFIRMED
    Order->>Notification: (SNS/SQS) OrderConfirmed
    Notification-->>Cliente: E-mail de confirmação
```

## Fluxo de compensação

```mermaid
sequenceDiagram
    participant Order as order-service
    participant Inventory as inventory-service
    participant Payment as payment-service
    participant Notification as notification-service

    Order->>Inventory: (SNS/SQS) OrderCreated
    Inventory->>Payment: (SNS/SQS) StockReserved
    Payment-->>Payment: Pagamento recusado
    Payment->>Order: (SNS/SQS) PaymentDeclined
    Order-->>Order: Status = CANCELLED
    Order->>Inventory: (SNS/SQS) OrderCancelled
    Inventory-->>Inventory: ReleaseStock (compensação)
    Order->>Notification: (SNS/SQS) OrderCancelled
    Notification-->>Notification: E-mail de cancelamento
```

## Caso alternativo: estoque indisponível

Se `inventory-service` não conseguir reservar estoque ao receber `OrderCreated`, publica `StockUnavailable` em vez de `StockReserved`. `order-service` escuta esse evento, cancela o pedido (`OrderCancelled`) e `notification-service` envia o e-mail de indisponibilidade/cancelamento. `payment-service` nunca chega a ser acionado nesse caminho.

## Regras que todo passo da Saga deve respeitar

- Cada transição de estado é local ao serviço dono do agregado (`order-service` é o único que muda o status do pedido).
- Toda publicação segue o Outbox Pattern; todo consumo verifica idempotência antes de agir (ver [`.claude/rules/comunicacao-eventos.md`](../../.claude/rules/comunicacao-eventos.md)).
- Falhas de negócio (`StockUnavailable`, `PaymentDeclined`) disparam compensação, não retry/DLQ.
