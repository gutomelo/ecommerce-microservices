---
name: comunicacao-eventos
description: Regras de eventos de domínio, contratos, Outbox Pattern e Idempotent Consumer
---

# Comunicação por eventos

- Todo contrato de evento vive **exclusivamente** em `platform-events`. Nenhum microsserviço declara sua própria versão de um evento existente — sempre importe a classe do `platform-events`.
- Todo evento é **imutável** e serializado em JSON. Todo evento estende `BaseEvent` e carrega no mínimo: `eventId`, `aggregateId`, `aggregateType`, `eventType`, `occurredAt`, `correlationId`, `traceId`, `version`, `payload`.
- Catálogo de eventos e tópicos SNS correspondentes: ver `docs/events/catalogo-eventos.md`. Não crie tópicos/eventos fora desse catálogo sem antes atualizar o catálogo e criar uma ADR.
- **Outbox Pattern obrigatório**: ao persistir uma mudança de estado que gera um evento, salve a entidade e o registro do evento na tabela `outbox` **na mesma transação**. Um worker separado lê a `outbox` e publica no SNS de forma assíncrona. **Nunca** publique diretamente no SNS dentro da transação de banco do caso de uso.
- **Idempotent Consumer obrigatório**: antes de processar um evento recebido, verifique a tabela `processed_events` pelo `eventId`. Se já processado, ignore a mensagem (ack sem reprocessar). Grave em `processed_events` como parte da mesma transação do processamento do evento.
- Cada microsserviço consumidor possui sua própria fila SQS dedicada, inscrita nos tópicos SNS relevantes (fan-out). Não compartilhe filas entre serviços.
- A publicação e o consumo de eventos nunca são reimplementados por serviço: use as abstrações de `platform-messaging` (`EventPublisher`, `EventConsumer`, `MessageSerializer`/`MessageDeserializer`).
