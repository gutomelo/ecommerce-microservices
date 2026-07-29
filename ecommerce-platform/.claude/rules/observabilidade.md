---
name: observabilidade
description: OpenTelemetry, Micrometer, Actuator, correlation ID e logging estruturado, padronizados via platform-observability
---

# Observabilidade

- Toda configuração de tracing (OpenTelemetry), métricas (Micrometer) e health check (Spring Boot Actuator) vem de `platform-observability` — nunca reconfigurar do zero em um serviço específico.
- Todo evento e toda requisição carregam `correlationId` e `traceId` propagados via MDC, presentes em todos os logs estruturados relacionados àquele fluxo.
- Logs são estruturados (JSON), nunca `System.out.println` ou log não estruturado.
- Toda Saga (fluxo de sucesso ou compensação) deve ser rastreável ponta a ponta pelo `correlationId` através de Jaeger/Zipkin e pelos logs de cada serviço envolvido.
