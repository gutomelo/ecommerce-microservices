# infrastructure/localstack

Simula AWS SNS/SQS localmente. É aqui que os 7 tópicos e as 4 filas (+ DLQs) da Saga são criados **automaticamente** — nenhum passo manual depois de `docker compose up`.

## O que faz

[`init/init-aws.sh`](init/init-aws.sh) roda sozinho assim que o container do LocalStack fica pronto (montado como hook em `/etc/localstack/init/ready.d/`, ver `docker-compose.yml`). Ele:

1. Cria os **7 tópicos SNS** do catálogo de eventos: `OrderCreated`, `StockReserved`, `StockUnavailable`, `PaymentApproved`, `PaymentDeclined`, `OrderConfirmed`, `OrderCancelled`.
2. Cria as **4 filas SQS**, cada uma com sua própria DLQ: `order-queue`, `inventory-queue`, `payment-queue`, `notification-queue`.
3. Assina cada fila nos tópicos certos (fan-out da Saga — ver tabela abaixo).
4. Autoriza o SNS a publicar em cada fila via *resource policy* (paridade com o comportamento de uma conta AWS real, não só "porque é LocalStack").

## Fan-out (quem assina o quê)

| Fila | Assina | Por quê |
|---|---|---|
| `order-queue` | `StockUnavailable`, `PaymentApproved`, `PaymentDeclined` | `order-service` reage a esses 3 para confirmar ou cancelar o pedido |
| `inventory-queue` | `OrderCreated`, `OrderCancelled` | `inventory-service` reserva ou libera estoque |
| `payment-queue` | `StockReserved` | `payment-service` só processa pagamento depois que há estoque reservado |
| `notification-queue` | todos os 7 | `notification-service` observa a Saga inteira |

## `maxReceiveCount` / `visibility timeout` por fila

| Fila | maxReceiveCount | visibility timeout | Por quê |
|---|---|---|---|
| `order-queue`, `inventory-queue`, `payment-queue` | 5 | 30s | Processamento rápido (escrita em banco ou decisão em memória) |
| `notification-queue` | 8 | 45s | Único handler que faz uma chamada de rede externa (SMTP para o Mailpit) — mais suscetível a falha transitória, por isso mais tentativas e mais tempo antes de redelivery |

Mensagem que esgota `maxReceiveCount` cai automaticamente na DLQ correspondente (`<fila>-dlq`) — nunca é descartada silenciosamente (ver [`.claude/rules/resiliencia.md`](../../.claude/rules/resiliencia.md)). Esse comportamento é provado contra um LocalStack real em `platform-messaging`'s `DlqRedeliveryIT` (ver [`platform/platform-messaging/README.md`](../../platform/platform-messaging/README.md)).

## Detalhe de implementação: por que Python dentro do script bash

A *resource policy* de cada fila (que autoriza o SNS a publicar nela) é um documento JSON dentro de outro JSON (`--attributes '{"Policy": "<json como string>"}'`). Escapar aspas manualmente em bash para isso é frágil e já quebrou duas vezes de formas diferentes durante o desenvolvimento — o script usa `python3` (já presente na imagem do LocalStack) só para montar esse arquivo de atributos corretamente via `json.dump`, em vez de escapar string na mão.

## Como inspecionar

```bash
docker exec localstack awslocal sns list-topics
docker exec localstack awslocal sqs list-queues
docker exec localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/notification-queue \
  --attribute-names All
```

## Referências

- Catálogo completo de eventos e tópicos: [`docs/events/catalogo-eventos.md`](../../docs/events/catalogo-eventos.md)
- Fluxo completo da Saga: [`docs/saga/fluxo-saga.md`](../../docs/saga/fluxo-saga.md)
- Guia de execução local: [`docs/deployment/local.md`](../../docs/deployment/local.md)
