# ecommerce-platform

Plataforma de e-commerce baseada em microsserviços, orientada a eventos (Event-Driven Architecture), construída como referência de boas práticas do ecossistema Spring para portfólio técnico.

> **Convenção de idioma:** todo o código (pacotes, classes, métodos, variáveis, chaves de configuração, mensagens de log/exceção, nomes de arquivos e pastas) é escrito em **inglês americano**. **Apenas** comentários, Javadoc explicativo, documentação (`docs/`, READMEs) e mensagens de commit são escritos em **português do Brasil**. Nunca misture os dois dentro do mesmo artefato (ex.: não crie uma classe `PedidoService`; crie `OrderService` com comentários em português quando necessário).

## Status atual

Este repositório está na fase de **Context Engineering**: ainda não existe código Java. O que existe hoje é o contexto que guiará as sessões futuras do Claude Code na construção do projeto (`CLAUDE.md`, `.claude/rules/`, `.claude/skills/`, `docs/`). Ao implementar, siga rigorosamente o que está descrito aqui e nos arquivos referenciados abaixo — não invente estrutura alternativa.

## Objetivo do projeto

Demonstrar uma arquitetura distribuída moderna, resiliente e desacoplada: um backend completo de e-commerce dividido em microsserviços autônomos, coordenados via **Saga por Coreografia** sobre **AWS SNS/SQS** (simulados localmente via **LocalStack**), seguindo **DDD**, **Clean Architecture**, **Arquitetura Hexagonal** e os doze fatores (**Twelve-Factor App**).

## Stack tecnológica

Java 21 · Spring Boot 3 · Spring Cloud · Spring Cloud AWS · Spring Cloud Gateway · Spring Cloud Config · Spring Data JPA · Spring Security + JWT · PostgreSQL · Flyway · Docker / Docker Compose · LocalStack · AWS SNS/SQS · OpenAPI · Lombok · MapStruct · Resilience4j · Micrometer · Actuator · JUnit 5 · Mockito · Testcontainers · OpenTelemetry · Prometheus · Grafana · Jaeger.

## Regras não negociáveis (resumo)

Detalhadas em `.claude/rules/`, sempre carregadas nesta pasta. As mais críticas:

- **Nunca** chamar REST entre microsserviços para regra de negócio. Toda comunicação entre serviços é assíncrona via SNS/SQS.
- Cada microsserviço tem **banco PostgreSQL exclusivo**. Nenhum serviço acessa o banco de outro.
- O módulo `platform/` **não contém regra de negócio** — apenas infraestrutura compartilhada.
- Todo evento publicado segue o **Outbox Pattern** (nunca publicar direto no SNS dentro da transação).
- Todo consumidor de evento é **idempotente** (tabela `ProcessedEvents`).
- Contratos de evento vivem **only** em `platform-events`; nenhum serviço cria sua própria versão de um evento existente.
- Resiliência obrigatória em toda comunicação assíncrona: Retry com backoff exponencial, Circuit Breaker, Timeout, Fallback, DLQ.

Veja o detalhamento completo em cada arquivo de `.claude/rules/`.

## Estrutura do monorepo (alvo)

```text
ecommerce-platform/
├── platform/                  # bibliotecas compartilhadas (sem regra de negócio)
│   ├── platform-common
│   ├── platform-events
│   ├── platform-security
│   ├── platform-messaging
│   ├── platform-observability
│   ├── platform-exception
│   ├── platform-testing
│   └── platform-bom
├── services/
│   ├── gateway-service/
│   ├── config-server/
│   ├── auth-service/
│   ├── customer-service/
│   ├── product-service/
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   └── notification-service/
├── infrastructure/
│   ├── docker/ · localstack/ · postgres/ · monitoring/
│   ├── prometheus/ · grafana/ · jaeger/ · scripts/
├── docs/
│   ├── architecture/ · diagrams/ · api/ · saga/ · events/ · deployment/ · decisions/
├── docker-compose.yml
├── pom.xml                    # Maven Multi Module raiz
└── README.md
```

> **Nota:** `.github/workflows/` (CI) vive na **raiz do repositório git** (`ecommerce-microservices/.github/workflows/`), não dentro de `ecommerce-platform/` — o GitHub só descobre workflows nesse caminho. Os steps do workflow usam `working-directory: ecommerce-platform` para rodar os comandos Maven.

Build: Maven Multi Module na raiz compila `platform/` + todos os `services/`. Cada serviço também compila isoladamente. Ambiente local sobe inteiro com `docker compose up` (LocalStack + SNS/SQS + Postgres por serviço + Gateway + Config Server + serviços + Prometheus + Grafana + Jaeger), sem passos manuais.

## Microsserviços e responsabilidades

| Serviço | Responsabilidade | Escuta | Publica |
|---|---|---|---|
| `gateway-service` | Roteamento, autenticação JWT, rate limiting, logging | — | — |
| `config-server` | Configuração centralizada | — | — |
| `auth-service` | Login, cadastro, JWT, refresh token | — | — |
| `customer-service` | CRUD de clientes | — | — |
| `product-service` | CRUD de produtos (id, nome, descrição, categoria, preço, estoque) | — | — |
| `order-service` | Cria pedidos, controla status (`PENDING`/`CONFIRMED`/`CANCELLED`), inicia a Saga. **Nunca** chama outro serviço diretamente | `PaymentApproved`, `PaymentDeclined` | `OrderCreated`, `OrderConfirmed`, `OrderCancelled` |
| `inventory-service` | Reserva/libera estoque | `OrderCreated`, `OrderCancelled` | `StockReserved`, `StockUnavailable` |
| `payment-service` | Processa pagamento (nunca acessa banco de outro serviço) | `StockReserved` | `PaymentApproved`, `PaymentDeclined` |
| `notification-service` | Envia e-mail/SMS | todos os eventos | — |

Fluxo completo (sucesso e compensação) em [`docs/saga/fluxo-saga.md`](docs/saga/fluxo-saga.md). Catálogo de eventos em [`docs/events/catalogo-eventos.md`](docs/events/catalogo-eventos.md).

## Onde encontrar mais detalhes

- `.claude/rules/` — regras obrigatórias por tema (idioma, arquitetura, eventos, resiliência, módulo platform, estrutura de serviço, banco de dados, segurança, observabilidade, testes). Carregadas automaticamente nesta pasta.
- `.claude/skills/` — skills para criar um novo microsserviço, adicionar um novo evento de domínio e registrar uma nova ADR, mantendo consistência com o padrão do projeto.
- `docs/architecture/visao-geral.md` — arquitetura detalhada.
- `docs/saga/fluxo-saga.md` — fluxo da Saga (sucesso e compensação).
- `docs/events/catalogo-eventos.md` — catálogo de eventos e contratos.
- `docs/decisions/` — Architecture Decision Records (ADRs).

## Definition of Done esperado

Cobertura de testes mínima de 80%, testes de saga e compensação, testes de retry e DLQ, Dockerfile e testes próprios por serviço, README por serviço, e toda regra de negócio isolada dentro do respectivo microsserviço.
