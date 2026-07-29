# payment-service

Decide, de forma simulada e determinística, se o pagamento de um pedido é aprovado ou recusado. Não existe provedor de pagamento real integrado — nem seria o objetivo de um projeto de portfólio local.

## Papel na arquitetura

Reage a `StockReservedEvent` (ou seja: só processa pagamento depois que `inventory-service` confirma que há estoque) e decide por um **limite de valor configurável** (`platform.payment.approval-threshold`, default `500.00`): valor do pedido menor ou igual ao limite → aprovado; acima → recusado. A regra em si (comparar com o limite) mora no domínio (`Payment.decide(...)`); o valor do limite é injeção de configuração, não uma constante no código — isso mantém a regra testável sem depender de propriedades externas.

Nunca acessa banco de outro serviço nem chama `order-service` de volta diretamente — só publica o resultado como evento (`PaymentApprovedEvent`/`PaymentDeclinedEvent`), e é `order-service` quem reage a ele para fechar o pedido.

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `GET` | `/payments/order/{orderId}` | `ADMIN`, `CUSTOMER` | Lista pagamentos de um pedido (somente leitura) |

Não existe endpoint de escrita — pagamentos só são criados em reação a `StockReservedEvent` consumido.

## Eventos

**Consome** (fila `payment-queue` — única fila que recebe apenas um tipo de evento, por isso não precisa de `EventTypeRouter`, usa `IdempotentEventDispatcher` direto):

| Evento | Reação |
|---|---|
| `StockReservedEvent` | `processPayment(orderId, totalAmount)` — decide aprovar/recusar |

**Publica** (via Outbox Pattern):

| Evento | Quando |
|---|---|
| `PaymentApprovedEvent` | `totalAmount <= platform.payment.approval-threshold` |
| `PaymentDeclinedEvent` | `totalAmount > platform.payment.approval-threshold` (motivo: `"valor do pedido acima do limite aprovado"`) |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `payments` | `id`, `order_id`, `amount`, `status` (`APPROVED`/`DECLINED`), `created_at`, `updated_at` |
| `outbox` | Outbox Pattern (mesma estrutura de `order-service`) |
| `processed_events` | Idempotent Consumer (mesma estrutura de `order-service`) |

## Configuração

| Propriedade | Default |
|---|---|
| `platform.payment.approval-threshold` | `500.00` — **a única propriedade de negócio específica deste serviço** |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5438` / `payment_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `payment` / `payment` |

## Como rodar isoladamente

```bash
docker compose up -d postgres-payment config-server localstack payment-service
```

Porta: **8086**. Swagger UI: `http://localhost:8086/swagger-ui.html`.

## Exemplo (curl, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s http://localhost:8080/payments/order/<order-id> -H "Authorization: Bearer $TOKEN"
```

Para gerar um pagamento `DECLINED` de propósito, crie um pedido em [`services/order-service`](../order-service/README.md) com `quantity * unitPrice` total acima de R$ 500,00.

## Testes

- `PaymentFlowIT.java` — contra PostgreSQL e LocalStack **reais** (Testcontainers): prova aprovação abaixo do limite, recusa acima, e idempotência sob redelivery da mesma mensagem.
- `StockReservedConsumerTest.java`, `PaymentServiceTest.java`, `OutboxPublisherWorkerTest.java` — casos de uso e worker, com mocks.
- `PaymentTest.java` — regra de decisão do domínio (`Payment.decide`).
