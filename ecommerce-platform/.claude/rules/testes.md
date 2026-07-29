---
name: testes
description: Padrão de testes obrigatório — unitários, integração, Testcontainers, cobertura mínima 80%
---

# Testes

- Cobertura mínima de **80%** por serviço.
- Testes unitários: JUnit 5 + Mockito, focados em `domain/` e `application/` (casos de uso), sem subir contexto Spring.
- Testes de integração: Testcontainers (PostgreSQL real, LocalStack para SNS/SQS quando aplicável) para `infrastructure/` e `api/`.
- Toda funcionalidade ligada à Saga precisa de teste específico:
  - fluxo de sucesso completo (publicação → consumo → próximo evento);
  - fluxo de compensação (ex.: `PaymentDeclinedEvent` → `OrderCancelledEvent` → liberação de estoque);
  - retry com backoff e efetivo envio à DLQ após esgotar tentativas;
  - idempotência (mesmo `eventId` processado duas vezes não duplica efeito).
- Utilize builders/factories/fakes de `platform-testing` em vez de recriá-los por serviço.
