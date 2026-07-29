# 0001. Saga por coreografia e comunicação exclusivamente via eventos

**Status:** Aceita

## Contexto

A plataforma precisa coordenar uma transação distribuída (criação de pedido → reserva de estoque → pagamento → confirmação) entre múltiplos microsserviços autônomos, cada um com banco de dados exclusivo, sem violar a independência entre eles nem introduzir um ponto único de falha.

## Decisão

- Toda comunicação entre microsserviços é assíncrona, via AWS SNS (publish/subscribe) e AWS SQS (fila por serviço consumidor). Chamadas síncronas (REST) entre serviços para regra de negócio são proibidas.
- A transação distribuída é resolvida via **Saga por Coreografia**: não há um serviço orquestrador central. Cada serviço reage a eventos recebidos e publica seus próprios eventos, incluindo eventos de compensação quando uma etapa falha.
- Confiabilidade da publicação é garantida via **Outbox Pattern**; duplicidade de processamento é evitada via **Idempotent Consumer**.

## Consequências

- **Fica mais fácil:** evoluir/escalar cada serviço de forma independente, adicionar novos consumidores de um evento existente sem tocar no publicador, e migrar para AWS real trocando apenas o endpoint do LocalStack.
- **Fica mais difícil:** visualizar o estado global de uma transação em andamento (mitigado por `correlationId` + tracing distribuído via Jaeger) e há maior complexidade de teste (Testcontainers + LocalStack, testes de compensação e de retry/DLQ).
- Consistência é **eventual**, não imediata — todo caso de uso precisa ser desenhado assumindo isso.

## Alternativas consideradas

- **Saga por Orquestração** (um serviço orquestrador central comandando os demais): rejeitada por introduzir acoplamento forte a um coordenador e um ponto único de falha, contrariando o objetivo de baixo acoplamento entre microsserviços.
- **Chamadas síncronas (REST/Feign) entre serviços com compensação manual**: rejeitada por acoplar disponibilidade dos serviços entre si e dificultar resiliência a falhas transitórias.
