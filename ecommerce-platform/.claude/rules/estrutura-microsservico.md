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
│   │       └── logback.xml
│   └── test/                    # testes unitários, integração, Testcontainers
├── Dockerfile
└── pom.xml
```

- `domain/` não importa Spring/JPA/AWS. `application/` depende só de `domain/` e das portas que define. `infrastructure/` implementa as portas de `application/`/`domain/`. `api/` depende de `application/`, nunca acessa `infrastructure/` diretamente.
- Todo serviço declara Dockerfile e testes próprios — nada de Dockerfile compartilhado entre serviços.
- Todo serviço depende do `platform/` conforme necessidade (ex.: `order-service` → `platform-events`, `platform-messaging`, `platform-common`, `platform-exception`); nunca duplica código já existente no `platform/`.
