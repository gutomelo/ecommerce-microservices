#!/bin/bash
# Cria os topicos SNS, filas SQS (com DLQ) e subscriptions do catalogo de eventos
# (ver docs/events/catalogo-eventos.md) assim que o LocalStack fica pronto.
# Roda automaticamente via hook /etc/localstack/init/ready.d/.
set -euo pipefail

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Valores default (order-queue/inventory-queue/payment-queue): processamento
# rapido e em memoria ou apenas escrita em banco (ver .claude/rules/resiliencia.md).
MAX_RECEIVE_COUNT=5
VISIBILITY_TIMEOUT=30

# notification-queue: unica fila cujo processamento faz uma chamada de rede
# externa (SMTP para o Mailpit) dentro do handler - mais suscetivel a falha
# transitoria, por isso mais tentativas e mais tempo antes de redelivery.
NOTIFICATION_MAX_RECEIVE_COUNT=8
NOTIFICATION_VISIBILITY_TIMEOUT=45

declare -a TOPICS=(
  "OrderCreated"
  "StockReserved"
  "StockUnavailable"
  "PaymentApproved"
  "PaymentDeclined"
  "OrderConfirmed"
  "OrderCancelled"
)

declare -A TOPIC_ARN
declare -A QUEUE_URL
declare -A QUEUE_ARN
declare -A QUEUE_TOPIC_ARNS

echo "==> Criando topicos SNS..."
for topic in "${TOPICS[@]}"; do
  arn=$(awslocal sns create-topic --name "$topic" --query 'TopicArn' --output text)
  TOPIC_ARN["$topic"]="$arn"
  echo "    $topic -> $arn"
done

create_queue_with_dlq() {
  local name=$1
  local max_receive_count=$2
  local visibility_timeout=$3
  local dlq_name="${name}-dlq"

  local dlq_url dlq_arn
  dlq_url=$(awslocal sqs create-queue --queue-name "$dlq_name" --query 'QueueUrl' --output text)
  dlq_arn=$(awslocal sqs get-queue-attributes --queue-url "$dlq_url" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

  local queue_url queue_arn
  queue_url=$(awslocal sqs create-queue --queue-name "$name" --query 'QueueUrl' --output text)
  queue_arn=$(awslocal sqs get-queue-attributes --queue-url "$queue_url" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

  local attrs_file="/tmp/${name}-attrs.json"
  cat > "$attrs_file" <<EOF
{
  "VisibilityTimeout": "${visibility_timeout}",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"${dlq_arn}\",\"maxReceiveCount\":\"${max_receive_count}\"}"
}
EOF
  awslocal sqs set-queue-attributes --queue-url "$queue_url" --attributes "file://${attrs_file}"

  QUEUE_URL["$name"]="$queue_url"
  QUEUE_ARN["$name"]="$queue_arn"
  echo "    $name -> $queue_url (DLQ: $dlq_name, maxReceiveCount=${max_receive_count}, visibilityTimeout=${visibility_timeout}s)"
}

echo "==> Criando filas SQS (com DLQ)..."
for queue in "order-queue" "inventory-queue" "payment-queue"; do
  create_queue_with_dlq "$queue" "$MAX_RECEIVE_COUNT" "$VISIBILITY_TIMEOUT"
done
create_queue_with_dlq "notification-queue" "$NOTIFICATION_MAX_RECEIVE_COUNT" "$NOTIFICATION_VISIBILITY_TIMEOUT"

subscribe() {
  local topic_name=$1
  local queue_name=$2
  local topic_arn="${TOPIC_ARN[$topic_name]}"
  local queue_arn="${QUEUE_ARN[$queue_name]}"

  local sub_arn
  sub_arn=$(awslocal sns subscribe \
    --topic-arn "$topic_arn" \
    --protocol sqs \
    --notification-endpoint "$queue_arn" \
    --query 'SubscriptionArn' --output text)
  awslocal sns set-subscription-attributes \
    --subscription-arn "$sub_arn" \
    --attribute-name RawMessageDelivery \
    --attribute-value true

  QUEUE_TOPIC_ARNS["$queue_name"]="${QUEUE_TOPIC_ARNS[$queue_name]:-} $topic_arn"
  echo "    $queue_name assinou $topic_name (RawMessageDelivery=true)"
}

echo "==> Assinando filas nos topicos (fan-out da Saga, ver docs/saga/fluxo-saga.md)..."
# order-service reage a falhas/aprovacoes que fecham ou cancelam o pedido
subscribe "StockUnavailable" "order-queue"
subscribe "PaymentApproved" "order-queue"
subscribe "PaymentDeclined" "order-queue"
# inventory-service reage a criacao e cancelamento de pedido
subscribe "OrderCreated" "inventory-queue"
subscribe "OrderCancelled" "inventory-queue"
# payment-service reage a reserva de estoque confirmada
subscribe "StockReserved" "payment-queue"
# notification-service observa toda a Saga
for topic in "${TOPICS[@]}"; do
  subscribe "$topic" "notification-queue"
done

echo "==> Autorizando SNS a publicar em cada fila (policy de recurso, paridade com AWS real)..."
for queue_name in "${!QUEUE_ARN[@]}"; do
  queue_arn="${QUEUE_ARN[$queue_name]}"
  queue_url="${QUEUE_URL[$queue_name]}"
  topic_arns="${QUEUE_TOPIC_ARNS[$queue_name]:-}"

  arn_json_array="["
  first=true
  for arn in $topic_arns; do
    if [ "$first" = true ]; then first=false; else arn_json_array="${arn_json_array},"; fi
    arn_json_array="${arn_json_array}\"${arn}\""
  done
  arn_json_array="${arn_json_array}]"

  # Policy document "puro" (sem escaping manual). O valor do atributo Policy
  # precisa ser uma string JSON dentro do JSON de --attributes; em vez de
  # escapar aspas na mao (fragil), usamos python3 (ja presente na imagem do
  # LocalStack) para gerar o arquivo de atributos corretamente.
  policy_inner_file="/tmp/${queue_name}-policy-inner.json"
  cat > "$policy_inner_file" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"Service": "sns.amazonaws.com"},
      "Action": "sqs:SendMessage",
      "Resource": "${queue_arn}",
      "Condition": {"ArnEquals": {"aws:SourceArn": ${arn_json_array}}}
    }
  ]
}
EOF

  attrs_file="/tmp/${queue_name}-policy-attrs.json"
  python3 - "$policy_inner_file" "$attrs_file" <<'PYEOF'
import json
import sys

policy_file, attrs_file = sys.argv[1], sys.argv[2]
with open(policy_file) as f:
    policy = f.read()
with open(attrs_file, "w") as f:
    json.dump({"Policy": policy}, f)
PYEOF

  awslocal sqs set-queue-attributes --queue-url "$queue_url" --attributes "file://${attrs_file}"
done

echo "==> LocalStack inicializado: $(echo ${!TOPIC_ARN[@]} | wc -w) topicos, $(echo ${!QUEUE_ARN[@]} | wc -w) filas + DLQs."
