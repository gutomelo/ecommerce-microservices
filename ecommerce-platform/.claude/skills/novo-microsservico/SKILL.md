---
name: novo-microsservico
description: Cria o esqueleto de um novo microsserviço do ecommerce-platform seguindo exatamente a estrutura padrão (Clean Architecture + Hexagonal), registrando-o no build Maven raiz e no docker-compose. Use quando o usuário pedir para "criar o serviço X", "scaffoldar um novo microsserviço" ou "adicionar order-service/payment-service/etc." ao monorepo.
---

# Criar um novo microsserviço

Antes de tudo, leia `CLAUDE.md` e `.claude/rules/estrutura-microsservico.md`, `.claude/rules/modulo-platform.md` e `.claude/rules/banco-de-dados.md` na raiz de `ecommerce-platform/` — este skill não repete essas regras, apenas aplica.

## Passos

1. **Confirme o nome e a responsabilidade** do serviço com base na tabela de microsserviços em `CLAUDE.md` (ex.: `order-service`, `inventory-service`). Não crie serviço fora dessa lista sem confirmar com o usuário.
2. Crie `services/<nome>-service/` com a estrutura exata de `.claude/rules/estrutura-microsservico.md`: `src/main/java/.../{domain,application,infrastructure,api,config}`, `src/main/resources/{db/migration,application.yml,logback.xml}`, `src/test/...`, `Dockerfile`, `pom.xml`.
3. No `pom.xml` do serviço: importe `platform-bom` como BOM, declare apenas as dependências de `platform/` que o serviço realmente usa (ver mapeamento de dependências em `CLAUDE.md`), sem fixar versões já centralizadas no BOM.
4. Adicione o módulo ao `<modules>` do `pom.xml` raiz do monorepo.
5. Crie o banco PostgreSQL exclusivo do serviço no `docker-compose.yml` (nome de container/volume próprio) e a primeira migration Flyway em `db/migration` (schema inicial, incluindo `outbox` e `processed_events` se o serviço publica ou consome eventos).
6. Registre o serviço no `docker-compose.yml` (build a partir do `Dockerfile`, variáveis de ambiente via Config Server, dependência dos serviços de infraestrutura: Postgres do serviço, LocalStack).
7. Se o serviço publica ou consome eventos: implemente os adapters em `infrastructure/` usando `EventPublisher`/`EventConsumer` de `platform-messaging`, nunca reimplementando serialização ou retry manualmente.
8. Escreva testes conforme `.claude/rules/testes.md` (unitários de domínio/aplicação + integração com Testcontainers) antes de considerar o serviço pronto.
9. Crie um `README.md` curto dentro do serviço (em português), descrevendo responsabilidade, eventos publicados/consumidos e como rodar isoladamente.

Não avance para o próximo microsserviço sem que o atual compile e os testes rodem.
