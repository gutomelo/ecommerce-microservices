---
name: banco-de-dados
description: Cada microsserviço tem banco PostgreSQL exclusivo, migrado via Flyway
---

# Banco de dados

- Cada microsserviço possui seu **próprio PostgreSQL**, isolado, com container e volume dedicados no `docker-compose.yml`.
- **Nunca** compartilhar schema ou instância de banco entre serviços. **Nunca** um serviço acessa diretamente a tabela de outro serviço — a única forma de obter dados de outro domínio é via evento consumido (e, quando necessário, mantido em uma projeção local).
- Todas as migrations são feitas via **Flyway**, versionadas em `src/main/resources/db/migration` de cada serviço.
- Tabelas técnicas obrigatórias por serviço que publica/consome eventos: `outbox` (Outbox Pattern) e `processed_events` (Idempotent Consumer) — ver `.claude/rules/comunicacao-eventos.md`.
