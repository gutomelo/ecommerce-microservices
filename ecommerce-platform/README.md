# ecommerce-platform

Plataforma de e-commerce baseada em microsserviços, orientada a eventos (Event-Driven Architecture), construída como referência de boas práticas do ecossistema Spring para portfólio técnico.

Backend distribuído de e-commerce com **9 microsserviços autônomos**, coordenados por uma **Saga por Coreografia** sobre **AWS SNS/SQS** (simulados localmente via **LocalStack**) — sem orquestrador central. Segue **DDD**, **Clean/Hexagonal Architecture** e os **Twelve-Factor App**.

## Stack

Java 21 · Spring Boot 3 · Spring Cloud (Config, Gateway) · Spring Cloud AWS (SNS/SQS) · Spring Data JPA · Spring Security + JWT · PostgreSQL · Flyway · Docker / Docker Compose · LocalStack · Resilience4j (Retry, Circuit Breaker, Rate Limiter) · Micrometer · OpenTelemetry · Prometheus · Grafana · Jaeger · Mailpit · JUnit 5 · Mockito · Testcontainers · JaCoCo.

## Arquitetura

```text
Cliente/Admin → gateway-service → { auth, customer, product, order, inventory, payment }-service
                                                              │
                                          order-service ──► SNS/SQS ◄── inventory-service
                                                              ▲              │
                                                              └── payment-service
                                                                       │
                                                          notification-service (assina tudo)
```

- **Saga por Coreografia**: `order-service` cria o pedido (`PENDING`) e publica `OrderCreated`. `inventory-service` reserva estoque e publica `StockReserved`/`StockUnavailable`. `payment-service` decide aprovar/recusar e publica `PaymentApproved`/`PaymentDeclined`. `order-service` reage a esses dois últimos e fecha o pedido (`CONFIRMED`/`CANCELLED`, com `inventory-service` liberando o estoque na compensação). `notification-service` observa tudo e envia e-mail/SMS.
- **Outbox Pattern** em todo serviço publicador (a mudança de estado e o registro do evento a publicar são commitados na mesma transação; um worker assíncrono publica de fato no SNS).
- **Idempotent Consumer** em todo serviço consumidor (tabela `processed_events` — reentrega do SQS nunca duplica efeito).
- **Retry + Circuit Breaker + DLQ**: publicação no SNS protegida por Resilience4j (retry com backoff + circuit breaker); toda fila SQS tem DLQ própria com `maxReceiveCount` configurado.
- Módulo `platform/` compartilha infraestrutura (eventos, mensageria, segurança, observabilidade, exceções, testes) sem nenhuma regra de negócio.

Diagramas C4, fluxo completo da Saga, catálogo de eventos e ADRs: ver [`docs/`](docs/) (índice abaixo).

## Como rodar

```bash
./mvnw clean package -DskipTests
docker compose build
docker compose up -d
```

Sobe sozinho: LocalStack (com os 7 tópicos SNS e 4 filas SQS/DLQ já criados), 7 PostgreSQL, `config-server`, `gateway-service`, os 7 microsserviços de negócio, Prometheus, Grafana, Jaeger e Mailpit. Um usuário `ADMIN` já vem semeado (`admin@ecommerce-platform.local` / `Admin@12345`).

Guia completo (fluxo de fumaça via `curl`, portas de cada serviço, como acessar Grafana/Jaeger/Mailpit): **[`docs/deployment/local.md`](docs/deployment/local.md)**.

## Testes

```bash
./mvnw clean verify
```

Testes unitários (JUnit 5 + Mockito) e de integração reais (Testcontainers: PostgreSQL e LocalStack de verdade — nunca mocks para infraestrutura) em todos os módulos. Gate de cobertura JaCoCo **≥ 80%** por módulo, obrigatório para o build passar. CI: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) (na raiz do repositório git, fora desta pasta).

## Microsserviços

| Serviço | Responsabilidade | Escuta | Publica |
|---|---|---|---|
| `gateway-service` | Roteamento, JWT, rate limiting | — | — |
| `config-server` | Configuração centralizada | — | — |
| `auth-service` | Login, cadastro, JWT, refresh token | — | — |
| `customer-service` | CRUD de clientes | — | — |
| `product-service` | CRUD de produtos | — | — |
| `order-service` | Cria pedidos, núcleo da Saga | `PaymentApproved`, `PaymentDeclined`, `StockUnavailable` | `OrderCreated`, `OrderConfirmed`, `OrderCancelled` |
| `inventory-service` | Reserva/libera estoque | `OrderCreated`, `OrderCancelled` | `StockReserved`, `StockUnavailable` |
| `payment-service` | Processa pagamento (simulado, por limite) | `StockReserved` | `PaymentApproved`, `PaymentDeclined` |
| `notification-service` | E-mail (Mailpit) / SMS (log) | todos os 7 eventos | — |

## Estrutura

```text
ecommerce-platform/
├── platform/       # bibliotecas compartilhadas (sem regra de negócio)
├── services/       # os 9 microsserviços
├── infrastructure/ # LocalStack init, config-repo, Prometheus, Grafana
├── docs/           # arquitetura, diagramas, eventos, saga, API, deployment, ADRs
├── docker-compose.yml
└── pom.xml         # Maven Multi-Module raiz
```

> `.github/workflows/` fica na raiz do repositório git (`ecommerce-microservices/`), não aqui dentro — ver nota em [`CLAUDE.md`](CLAUDE.md).

## Documentação

- [`CLAUDE.md`](CLAUDE.md) — visão geral para desenvolvimento assistido por IA (stack, regras, estrutura).
- [`docs/architecture/visao-geral.md`](docs/architecture/visao-geral.md) — arquitetura detalhada.
- [`docs/diagrams/`](docs/diagrams/) — diagramas C4 (contexto e contêineres).
- [`docs/saga/fluxo-saga.md`](docs/saga/fluxo-saga.md) — fluxo completo da Saga (sucesso e compensação).
- [`docs/events/catalogo-eventos.md`](docs/events/catalogo-eventos.md) — catálogo de eventos, tópicos e filas.
- [`docs/api/`](docs/api/) — especificações OpenAPI de cada serviço.
- [`docs/deployment/local.md`](docs/deployment/local.md) — guia de execução local.
- [`docs/deployment/aws.md`](docs/deployment/aws.md) — notas de migração para AWS real.
- [`docs/decisions/`](docs/decisions/) — Architecture Decision Records.
