---
name: novo-evento-dominio
description: Adiciona um novo evento de domínio ao platform-events, atualiza o catálogo de eventos e liga produtor/consumidor. Use quando o usuário pedir para "criar um novo evento", "adicionar um evento à saga" ou nomear um evento específico (ex. "preciso de um evento EstoqueAjustado").
---

# Adicionar um novo evento de domínio

Leia primeiro `.claude/rules/comunicacao-eventos.md` e `docs/events/catalogo-eventos.md`.

## Passos

1. Confirme que o evento realmente não existe no catálogo (`docs/events/catalogo-eventos.md`) nem em `platform-events`. Se já existir algo equivalente, reutilize — não crie duplicata.
2. Crie a classe imutável do evento em `platform-events`, estendendo `BaseEvent`, com os campos obrigatórios (`eventId`, `aggregateId`, `aggregateType`, `eventType`, `occurredAt`, `correlationId`, `traceId`, `version`, `payload`) e o payload específico do evento.
3. Atualize `docs/events/catalogo-eventos.md`: nome do evento, tópico SNS, serviço publicador, serviço(s) consumidor(es), propósito, campos do payload.
4. Se o evento introduz um novo tópico SNS ou fila SQS: atualize também a configuração de infraestrutura (LocalStack/`docker-compose.yml`) e a fila do(s) serviço(s) consumidor(es), incluindo DLQ e retry conforme `.claude/rules/resiliencia.md`.
5. No serviço publicador: publique o evento através do Outbox Pattern (salvar entidade + registro na tabela `outbox` na mesma transação) — nunca publicar direto no SNS.
6. No(s) serviço(s) consumidor(es): implemente o listener verificando idempotência via `processed_events` antes de processar, conforme `.claude/rules/comunicacao-eventos.md`.
7. Se o evento faz parte do fluxo da Saga (sucesso ou compensação), atualize também `docs/saga/fluxo-saga.md` com o novo passo.
8. Escreva teste de contrato do evento (serialização/desserialização) e teste do produtor/consumidor conforme `.claude/rules/testes.md`.
