# notification-service

Observa toda a Saga e envia e-mail/SMS ao cliente. É o único serviço que assina os 7 tópicos do catálogo de eventos — e o único que não publica nada.

## Papel na arquitetura

Diferente de todo outro serviço de negócio deste projeto, **não tem API REST nenhuma** (nem de leitura) — não há `SecurityConfig`, nem dependência de `platform-security`, porque não existe nenhum endpoint a proteger. É consumidor puro de eventos.

Consome os 7 eventos do catálogo, mas só 3 deles carregam `customerId` no payload — `OrderCreatedEvent`, `OrderConfirmedEvent` e `OrderCancelledEvent` — e são esses os únicos que geram notificação de verdade:

| Evento | Notificação |
|---|---|
| `OrderCreatedEvent` | E-mail "Recebemos seu pedido" |
| `OrderConfirmedEvent` | E-mail "Pedido confirmado" + SMS |
| `OrderCancelledEvent` | E-mail "Pedido cancelado" (com motivo) + SMS |
| `StockReservedEvent`, `StockUnavailableEvent`, `PaymentApprovedEvent`, `PaymentDeclinedEvent` | Só logado (`logIntermediateSagaEvent`) — sem destinatário conhecido aqui |

Os outros 4 eventos são passos **intermediários** da Saga: nenhum carrega `customerId`, e não há chamada síncrona permitida a outro serviço para descobrir o cliente (ver [`.claude/rules/arquitetura.md`](../../.claude/rules/arquitetura.md)). Isso não é uma lacuna — o desfecho de qualquer falha intermediária (estoque indisponível ou pagamento recusado) sempre chega ao cliente via `OrderCancelledEvent`, que `order-service` já publica com `customerId` e o motivo correto.

Como não há integração real com o cadastro do cliente, o endereço de e-mail é **derivado deterministicamente** do `customerId` (`customer-<uuid>@ecommerce-platform.local`) — o Mailpit aceita qualquer destinatário, então o que se prova aqui é o pipeline evento → e-mail, não uma integração real de contatos (ver [ADR 0002](../../docs/decisions/0002-mailpit-para-email-local-e-log-para-sms.md)).

## Endpoints

Nenhum.

## Eventos

**Consome** (fila `notification-queue`, assina os 7 tópicos, roteados por `EventTypeRouter`): `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`, `StockReservedEvent`, `StockUnavailableEvent`, `PaymentApprovedEvent`, `PaymentDeclinedEvent`.

**Publica**: nada.

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `processed_events` | Idempotent Consumer — única tabela deste serviço; **não há** tabela `outbox` (nunca publica eventos) |

## Configuração

| Propriedade | Default local | Descrição |
|---|---|---|
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP do Mailpit (em Docker: `mailpit`/`1025`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5439` / `notification_db` | |
| `DB_USERNAME` / `DB_PASSWORD` | `notification` / `notification` | |

## Como rodar isoladamente

```bash
docker compose up -d postgres-notification config-server localstack mailpit notification-service
```

Porta: **8087** (só para Actuator — `/actuator/health`, sem API de negócio).

## Como ver o resultado

Sem endpoint próprio para chamar — a forma de observar é criar um pedido em [`services/order-service`](../order-service/README.md) e checar o Mailpit:

```bash
# UI web
open http://localhost:8026

# ou via API do Mailpit
curl -s http://localhost:8026/api/v1/messages | python3 -m json.tool
```

O SMS é só logado — visível em `docker logs notification-service`.

## Testes

- `NotificationFlowIT.java` — contra PostgreSQL e LocalStack **reais** (Testcontainers): prova que os 3 eventos com `customerId` geram e-mail/SMS, os 4 intermediários não geram nenhum, e idempotência sob redelivery. `EmailSender`/`SmsSender` são substituídos por mocks (`@MockitoBean`) neste teste — a entrega real via Mailpit é validada separadamente.
- `NotificationQueueConsumersTest.java`, `NotificationServiceTest.java` — casos de uso, com mocks.
- `JavaMailEmailSenderTest.java`, `LoggingSmsSenderTest.java` — adaptadores de envio.
