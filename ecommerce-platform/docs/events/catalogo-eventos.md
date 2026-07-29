# Catálogo de eventos

Todos os eventos abaixo são definidos **exclusivamente** em `platform-events`, estendem `BaseEvent` e carregam: `eventId`, `aggregateId`, `aggregateType`, `eventType`, `occurredAt`, `correlationId`, `traceId`, `version`, `payload`.

| Evento (código) | Tópico SNS | Termo de negócio (pt-BR) | Publicado por | Consumido por | Propósito |
|---|---|---|---|---|---|
| `OrderCreatedEvent` | `OrderCreated` | Pedido Criado | `order-service` | `inventory-service` | Novo pedido criado, dispara a Saga |
| `StockReservedEvent` | `StockReserved` | Estoque Reservado | `inventory-service` | `payment-service` | Estoque reservado com sucesso para o pedido. Carrega `totalAmount` (copiado de `OrderCreatedEvent`) porque `payment-service` não consome `OrderCreatedEvent` diretamente |
| `StockUnavailableEvent` | `StockUnavailable` | Estoque Indisponível | `inventory-service` | `order-service` | Não há estoque suficiente; dispara compensação |
| `PaymentApprovedEvent` | `PaymentApproved` | Pagamento Aprovado | `payment-service` | `order-service` | Pagamento aprovado; pedido pode ser confirmado |
| `PaymentDeclinedEvent` | `PaymentDeclined` | Pagamento Recusado | `payment-service` | `order-service` | Pagamento recusado; dispara compensação |
| `OrderConfirmedEvent` | `OrderConfirmed` | Pedido Confirmado | `order-service` | `notification-service` | Pedido confirmado; notificar cliente |
| `OrderCancelledEvent` | `OrderCancelled` | Pedido Cancelado | `order-service` | `inventory-service`, `notification-service` | Pedido cancelado; liberar estoque e notificar |

Os nomes de código (classe e tópico) são a fonte da verdade e seguem inglês americano, conforme [`.claude/rules/idioma-e-estilo.md`](../../.claude/rules/idioma-e-estilo.md). A coluna "Termo de negócio (pt-BR)" existe apenas para documentação e rastreabilidade com a linguagem ubíqua do domínio — nunca use esses termos em identificadores de código.

`notification-service` está inscrito em todos os tópicos acima (fila própria por consumidor).

## Filas SQS

| Fila | Serviço dono | DLQ | Retry |
|---|---|---|---|
| Order Queue | `order-service` | Sim | Backoff exponencial |
| Inventory Queue | `inventory-service` | Sim | Backoff exponencial |
| Payment Queue | `payment-service` | Sim | Backoff exponencial |
| Notification Queue | `notification-service` | Sim | Backoff exponencial |

Cada fila tem `visibility timeout` e `maxReceiveCount` configurados; mensagens que excedem o número máximo de tentativas vão automaticamente para a respectiva DLQ (ver [`.claude/rules/resiliencia.md`](../../.claude/rules/resiliencia.md)).

## Adicionando um novo evento

Use o skill `novo-evento-dominio` (`.claude/skills/novo-evento-dominio/SKILL.md`) para manter este catálogo, o `platform-events` e a documentação da Saga sincronizados.
