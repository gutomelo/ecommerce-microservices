# order-service

O núcleo da Saga. Cria pedidos e é quem decide, no fim, se um pedido fica `CONFIRMED` ou `CANCELLED` — mas nunca chama `inventory-service` ou `payment-service` diretamente para isso.

## Papel na arquitetura

`order-service` **inicia** a Saga por Coreografia (ver [`docs/saga/fluxo-saga.md`](../../docs/saga/fluxo-saga.md)) e **fecha** o pedido em reação aos eventos que os outros serviços publicam — nunca orquestra nada ativamente. Fluxo:

1. `POST /orders` cria o pedido como `PENDING` e publica `OrderCreatedEvent` (via Outbox Pattern).
2. `inventory-service` reage, reserva estoque, publica `StockReservedEvent` ou `StockUnavailableEvent`.
3. Se reservado, `payment-service` reage, decide aprovar/recusar, publica `PaymentApprovedEvent` ou `PaymentDeclinedEvent`.
4. `order-service` reage a esses eventos e fecha o pedido: `PaymentApprovedEvent` → `confirm()` (publica `OrderConfirmedEvent`); `PaymentDeclinedEvent` ou `StockUnavailableEvent` → `cancel()` (publica `OrderCancelledEvent`, que por sua vez faz `inventory-service` liberar o estoque reservado).

Todo o pipeline usa **Outbox Pattern** (a mudança de estado e o evento a publicar são salvos na mesma transação; um worker assíncrono publica de fato no SNS) e **Idempotent Consumer** (tabela `processed_events` — reentrega do SQS nunca duplica um `confirm()`/`cancel()`). Há ainda uma segunda camada de proteção no próprio domínio: `confirm()`/`cancel()` só têm efeito se o pedido ainda estiver `PENDING` (no-op silencioso caso contrário, útil contra corrida entre eventos).

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `POST` | `/orders` | `ADMIN`, `CUSTOMER` | Cria pedido (dispara a Saga) |
| `GET` | `/orders/{id}` | `ADMIN`, `CUSTOMER` | Consulta status do pedido |

Diferente de `customer-service`/`product-service`: aqui `CUSTOMER` também pode escrever (criar pedido), já que é o próprio cliente quem inicia a Saga — não há restrição de escrita a `ADMIN`.

Não há `PUT`/`DELETE`: o status de um pedido só muda por reação a eventos consumidos, nunca via API.

## Eventos

**Publica** (via Outbox Pattern):

| Evento | Quando |
|---|---|
| `OrderCreatedEvent` | Ao criar o pedido |
| `OrderConfirmedEvent` | Ao confirmar (pagamento aprovado) |
| `OrderCancelledEvent` | Ao cancelar (pagamento recusado ou estoque indisponível) |

**Consome** (fila `order-queue`, roteadas por `EventTypeRouter`):

| Evento | Reação |
|---|---|
| `PaymentApprovedEvent` | `confirm()` |
| `PaymentDeclinedEvent` | `cancel()` |
| `StockUnavailableEvent` | `cancel()` |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `orders` | `id`, `customer_id`, `total_amount`, `status` (`PENDING`/`CONFIRMED`/`CANCELLED`), `created_at`, `updated_at` |
| `order_items` | `id`, `order_id` (FK), `product_id`, `quantity`, `unit_price` |
| `outbox` | Outbox Pattern: `id`, `aggregate_id`, `event_type`, `topic`, `payload`, `status` (`PENDING`/`PUBLISHED`), `created_at`, `published_at` |
| `processed_events` | Idempotent Consumer: `event_id`, `processed_at` |

## Configuração

| Propriedade | Default local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5436` / `order_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `order_service` / `order_service` |
| `platform.outbox.publish-interval-millis` | `2000` (worker do outbox roda a cada 2s) |

## Como rodar isoladamente

```bash
docker compose up -d postgres-order config-server localstack order-service
```

Porta: **8084**. Swagger UI: `http://localhost:8084/swagger-ui.html`.

## Exemplos (curl, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# criar pedido (valor total <= R$500 aprova automaticamente no payment-service;
# acima disso dispara a compensacao - ver services/payment-service/README.md)
curl -s -X POST http://localhost:8080/orders -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"<uuid-do-cliente>","items":[{"productId":"<uuid-do-produto>","quantity":2,"unitPrice":19.90}]}'
# -> status "PENDING"

# alguns segundos depois (Saga assincrona: reserva -> pagamento -> confirmacao)
curl -s http://localhost:8080/orders/<id-do-pedido> -H "Authorization: Bearer $TOKEN"
# -> status "CONFIRMED" (ou "CANCELLED", se o valor exceder o limite de aprovacao)
```

## Testes

- `OrderFlowIT.java` — prova, contra PostgreSQL e LocalStack **reais** (Testcontainers): outbox publicando de fato no SNS, e consumo idempotente de `PaymentApprovedEvent` via `order-queue` (redelivery da mesma mensagem não duplica a confirmação).
- `OrderQueueConsumersTest.java`, `OrderServiceTest.java`, `OutboxPublisherWorkerTest.java` — casos de uso e worker, com mocks.
- `OrderItemTest.java`, `OrderTest.java` — regras do agregado de domínio (transições de estado válidas/inválidas).
