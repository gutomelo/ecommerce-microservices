# Catálogo de eventos

Todos os eventos abaixo são definidos **exclusivamente** em `platform-events`, estendem `BaseEvent` e carregam: `eventId`, `aggregateId`, `aggregateType`, `eventType`, `occurredAt`, `correlationId`, `traceId`, `version`, `payload`.

| Evento | Tópico SNS | Publicado por | Consumido por | Propósito |
|---|---|---|---|---|
| `PedidoCriadoEvent` | `PedidoCriado` | `order-service` | `inventory-service` | Novo pedido criado, dispara a Saga |
| `EstoqueReservadoEvent` | `EstoqueReservado` | `inventory-service` | `payment-service` | Estoque reservado com sucesso para o pedido |
| `EstoqueIndisponivelEvent` | `EstoqueIndisponivel` | `inventory-service` | `order-service` | Não há estoque suficiente; dispara compensação |
| `PagamentoAprovadoEvent` | `PagamentoAprovado` | `payment-service` | `order-service` | Pagamento aprovado; pedido pode ser confirmado |
| `PagamentoRecusadoEvent` | `PagamentoRecusado` | `payment-service` | `order-service` | Pagamento recusado; dispara compensação |
| `PedidoConfirmadoEvent` | `PedidoConfirmado` | `order-service` | `notification-service` | Pedido confirmado; notificar cliente |
| `PedidoCanceladoEvent` | `PedidoCancelado` | `order-service` | `inventory-service`, `notification-service` | Pedido cancelado; liberar estoque e notificar |

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
