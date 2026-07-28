---
name: estrutura-microsservico
description: Estrutura de pastas obrigatória, idêntica para todos os microsserviços
---

# Estrutura de um microsserviço

Todo serviço em `services/<nome>-service/` segue exatamente esta organização (sem exceções, sem pastas extras na raiz do `src/main/java`):

```text
<nome>-service/
├── src/
│   ├── main/
│   │   ├── java/.../
│   │   │   ├── domain/          # entidades, value objects, agregados, domain events, ports de repositório
│   │   │   ├── application/     # casos de uso, ports de entrada/saída, DTOs de aplicação
│   │   │   ├── infrastructure/  # adapters: JPA, mensageria (SNS/SQS), clients, outbox, idempotência
│   │   │   ├── api/              # controllers REST, request/response DTOs, mappers (MapStruct)
│   │   │   └── config/          # configuração Spring específica do serviço
│   │   └── resources/
│   │       ├── db/              # migrations Flyway
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/                    # testes unitários, integração, Testcontainers
├── Dockerfile
└── pom.xml
```

- `domain/` não importa Spring/JPA/AWS. `application/` depende só de `domain/` e das portas que define. `infrastructure/` implementa as portas de `application/`/`domain/`. `api/` depende de `application/`, nunca acessa `infrastructure/` diretamente.
- Todo serviço declara Dockerfile e testes próprios — nada de Dockerfile compartilhado entre serviços.
- Todo serviço depende do `platform/` conforme necessidade (ex.: `order-service` → `platform-events`, `platform-messaging`, `platform-common`, `platform-exception`); nunca duplica código já existente no `platform/`.

## Exceção: serviços sem domínio de negócio

`gateway-service` e `config-server` não têm agregados/casos de uso próprios — são infraestrutura pura (roteamento e configuração centralizada). Para esses dois, **não** force os pacotes `domain/`/`application/` vazios; use apenas `infrastructure/`, `api/` (rotas, quando aplicável) e `config/`. Todos os demais serviços seguem a estrutura completa acima sem exceção.

## Exceção: `gateway-service` é reativo (WebFlux)

Spring Cloud Gateway roda sobre WebFlux/Netty, não sobre Servlet. Por isso:

- `JwtAuthenticationFilter` e `CorrelationIdFilter` de `platform-security`/`platform-observability` são `OncePerRequestFilter` (Servlet) e **não se aplicam** ao `gateway-service` — eles continuam válidos para todos os demais serviços (Spring MVC).
- O `gateway-service` implementa seus próprios `GlobalFilter`/`WebFilter` reativos, reaproveitando apenas a lógica de negócio de `platform-security` que não é atada a Servlet (`JwtTokenProvider`, `Roles`, `SecurityConstants` — puro Java, sem `HttpServletRequest`). Isso é o Ports and Adapters funcionando como esperado: a lógica compartilhada é reutilizada, o adapter de transporte é específico de cada serviço.
