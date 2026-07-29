# Visão geral da arquitetura

## Objetivo

Backend de e-commerce composto por microsserviços autônomos, comunicando-se exclusivamente por eventos, coordenados por uma **Saga por Coreografia**, sem coordenador central.

## Princípios adotados

- **Domain-Driven Design (DDD):** cada microsserviço modela seu próprio domínio, com linguagem ubíqua própria. Os nomes de código (classes, eventos, tópicos) são sempre em inglês americano; o termo de negócio equivalente em português só aparece em documentação (ver [`.claude/rules/idioma-e-estilo.md`](../../.claude/rules/idioma-e-estilo.md) e o [catálogo de eventos](../events/catalogo-eventos.md)).
- **Clean Architecture / Ports and Adapters (Hexagonal):** camadas `domain → application → infrastructure/api`, com dependências sempre apontando para dentro (infra depende de domínio, nunca o contrário).
- **SOLID / Clean Code:** classes pequenas, responsabilidade única, injeção de dependência via construtor.
- **Twelve-Factor App:** configuração externalizada (Config Server), stateless, logs como stream, paridade dev/prod via Docker Compose + LocalStack.
- **Event-Driven Architecture:** todo acoplamento entre serviços é assíncrono, via AWS SNS (pub/sub) e AWS SQS (fila por consumidor).

## Por que Saga por Coreografia (e não orquestração)

Evita um orquestrador central que se tornaria um ponto único de falha e de acoplamento. Cada serviço decide reagir a um evento e publicar o próximo, com o `correlationId` amarrando toda a transação distribuída para fins de observabilidade. Ver ADR [0001](../decisions/0001-saga-por-coreografia-e-comunicacao-via-eventos.md).

## Padrões de confiabilidade

- **Outbox Pattern:** garante que a persistência do estado e a publicação do evento nunca fiquem inconsistentes entre si (o evento só é considerado "a publicar" se a transação de negócio foi commitada).
- **Idempotent Consumer:** garante que reentregas do SNS/SQS (at-least-once delivery) não dupliquem efeitos colaterais.
- **Retry + Circuit Breaker + Timeout + Fallback + DLQ:** garantem resiliência a falhas transitórias sem perda de mensagens e sem cascata de falhas.

## Módulo Platform

Biblioteca compartilhada de infraestrutura (sem regra de negócio), consumida via Maven por todos os microsserviços. Detalhes em [`.claude/rules/modulo-platform.md`](../../.claude/rules/modulo-platform.md).

## Ambiente local

Todo o ambiente (microsserviços, Postgres por serviço, LocalStack simulando SNS/SQS, Prometheus, Grafana, Jaeger) sobe com um único `docker compose up`, permitindo migração posterior para AWS real com alteração mínima de configuração (troca de endpoint do LocalStack pelos endpoints reais da AWS).

## Referências

- Diagramas C4 (contexto e contêineres): [`docs/diagrams/`](../diagrams/)
- Fluxo detalhado da Saga: [`docs/saga/fluxo-saga.md`](../saga/fluxo-saga.md)
- Catálogo de eventos: [`docs/events/catalogo-eventos.md`](../events/catalogo-eventos.md)
- Decisões arquiteturais: [`docs/decisions/`](../decisions/)
- Especificações OpenAPI de cada serviço: [`docs/api/`](../api/)
- Guias de execução local e migração para AWS real: [`docs/deployment/`](../deployment/)
