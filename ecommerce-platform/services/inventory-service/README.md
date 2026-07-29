# inventory-service

Reserva e libera estoque em reação aos eventos da Saga. Não tem nenhum endpoint de escrita — todo estado muda só por evento consumido.

## Papel na arquitetura

Mantém seu **próprio** ledger de estoque (`stock_items`), desacoplado do campo `stock` de `product-service` (cada serviço é dono do seu dado, ver [`.claude/rules/banco-de-dados.md`](../../.claude/rules/banco-de-dados.md)). Produtos nunca vistos antes são provisionados automaticamente com uma quantidade default (100 unidades) na primeira tentativa de reserva — isso evita que o serviço precise de sincronização síncrona com o catálogo para o fluxo de demonstração funcionar.

A reserva é **tudo ou nada**: se qualquer item do pedido não tiver estoque suficiente, nenhum item é reservado, e o serviço publica `StockUnavailableEvent` (o pedido inteiro é recusado, nunca parcialmente atendido). Reservas bem-sucedidas são guardadas em `stock_reservations` — é esse registro que permite saber exatamente o que devolver quando `OrderCancelledEvent` chega (compensação).

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `GET` | `/stock/{productId}` | `ADMIN`, `CUSTOMER` | Consulta quantidade disponível (somente leitura) |

Não existe endpoint de escrita — estoque só muda via `OrderCreatedEvent`/`OrderCancelledEvent` consumidos.

## Eventos

**Consome** (fila `inventory-queue`, roteadas por `EventTypeRouter`):

| Evento | Reação |
|---|---|
| `OrderCreatedEvent` | Tenta reservar estoque de cada item do pedido |
| `OrderCancelledEvent` | Libera (devolve) o estoque reservado para aquele pedido |

**Publica** (via Outbox Pattern, só em reação a `OrderCreatedEvent`):

| Evento | Quando |
|---|---|
| `StockReservedEvent` | Todos os itens tinham estoque suficiente |
| `StockUnavailableEvent` | Pelo menos um item não tinha estoque suficiente (motivo: `"estoque insuficiente"`) |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `stock_items` | `product_id` (PK), `available_quantity` |
| `stock_reservations` | `id`, `order_id` (único), `created_at` — registro de uma reserva bem-sucedida |
| `stock_reservation_items` | `id`, `reservation_id` (FK), `product_id`, `quantity` — o que foi reservado, item a item |
| `outbox` | Outbox Pattern (mesma estrutura de `order-service`) |
| `processed_events` | Idempotent Consumer (mesma estrutura de `order-service`) |

## Configuração

| Propriedade | Default local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5437` / `inventory_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `inventory` / `inventory` |
| `platform.outbox.publish-interval-millis` | `2000` |

## Como rodar isoladamente

```bash
docker compose up -d postgres-inventory config-server localstack inventory-service
```

Porta: **8085**. Swagger UI: `http://localhost:8085/swagger-ui.html`.

## Exemplo (curl, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# consultar estoque de um produto (100 se nunca reservado antes - default de provisionamento)
curl -s http://localhost:8080/stock/<product-id> -H "Authorization: Bearer $TOKEN"
```

Para ver a quantidade mudar de verdade, crie um pedido em [`services/order-service`](../order-service/README.md) com esse `productId` e consulte de novo alguns segundos depois.

## Testes

- `InventoryFlowIT.java` — contra PostgreSQL e LocalStack **reais** (Testcontainers): prova reserva bem-sucedida, `StockUnavailableEvent` quando a quantidade excede o disponível, liberação de estoque via `OrderCancelledEvent` (compensação), e idempotência sob redelivery.
- `InventoryQueueConsumersTest.java`, `InventoryServiceTest.java`, `OutboxPublisherWorkerTest.java` — casos de uso e worker, com mocks.
- `StockItemTest.java`, `StockReservationTest.java` — regras do agregado de domínio.
