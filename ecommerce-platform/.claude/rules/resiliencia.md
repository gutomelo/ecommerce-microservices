---
name: resiliencia
description: Retry, Circuit Breaker, Timeout, Fallback e Dead Letter Queue obrigatórios na mensageria
---

# Resiliência

- Toda chamada a um recurso externo (SNS, SQS, banco, outro serviço via Gateway) que possa falhar transitoriamente deve ter **Retry com backoff exponencial** (Resilience4j).
- Toda integração propensa a falha em cascata deve ter **Circuit Breaker** configurado (Resilience4j), com fallback definido — nunca deixe uma chamada travar o thread indefinidamente sem **Timeout**.
- Cada fila SQS tem: **Dead Letter Queue** própria, número máximo de tentativas (`maxReceiveCount`) e `visibility timeout` configurados. Mensagens que excedem o máximo de tentativas vão automaticamente para a DLQ — nunca descarte mensagens silenciosamente.
- Falhas de negócio esperadas (ex.: `EstoqueIndisponivel`, `PagamentoRecusado`) não são erros técnicos: são publicadas como eventos de compensação da própria Saga, não devem gerar retry nem ir para DLQ.
- Falhas técnicas (erro de serialização, indisponibilidade momentânea de infraestrutura) seguem o fluxo de retry/DLQ normal.
