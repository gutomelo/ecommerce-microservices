# Replica 1:1 o catalogo de eventos (docs/events/catalogo-eventos.md) e o script
# de bootstrap do LocalStack (infrastructure/localstack/init/init-aws.sh):
# mesmos 7 topicos SNS, mesmas 4 filas SQS + DLQ, mesmas subscriptions de fan-out.
# Nenhum código do platform-messaging muda - só o endpoint/credenciais (ver
# docs/deployment/aws.md).

locals {
  name_prefix = "${var.project}-${var.environment}"

  topics = [
    "OrderCreated",
    "StockReserved",
    "StockUnavailable",
    "PaymentApproved",
    "PaymentDeclined",
    "OrderConfirmed",
    "OrderCancelled",
  ]

  # Fila -> config de resiliencia (ver .claude/rules/resiliencia.md). notification-queue
  # tem mais tentativas/timeout porque seu handler faz uma chamada de rede externa (SES).
  queues = {
    "order-queue" = {
      max_receive_count  = 5
      visibility_timeout = 30
    }
    "inventory-queue" = {
      max_receive_count  = 5
      visibility_timeout = 30
    }
    "payment-queue" = {
      max_receive_count  = 5
      visibility_timeout = 30
    }
    "notification-queue" = {
      max_receive_count  = 8
      visibility_timeout = 45
    }
  }

  # Fan-out da Saga (ver "Assinando filas nos topicos" em init-aws.sh) - cada
  # par (topico, fila) vira uma aws_sns_topic_subscription.
  subscriptions = flatten([
    for pair in [
      { queue = "order-queue", topic = "StockUnavailable" },
      { queue = "order-queue", topic = "PaymentApproved" },
      { queue = "order-queue", topic = "PaymentDeclined" },
      { queue = "inventory-queue", topic = "OrderCreated" },
      { queue = "inventory-queue", topic = "OrderCancelled" },
      { queue = "payment-queue", topic = "StockReserved" },
    ] : [pair]
    ]
  )

  # notification-service assina todos os 7 topicos (ver catalogo de eventos).
  notification_subscriptions = [for topic in local.topics : { queue = "notification-queue", topic = topic }]

  all_subscriptions = concat(local.subscriptions, local.notification_subscriptions)

  # Agrupa os ARNs de topico que cada fila assina, para a queue policy
  # (equivalente ao "Autorizando SNS a publicar em cada fila" do init-aws.sh).
  queue_topic_names = {
    for queue_name in keys(local.queues) :
    queue_name => [for s in local.all_subscriptions : s.topic if s.queue == queue_name]
  }
}

resource "aws_sns_topic" "this" {
  for_each = toset(local.topics)

  name = each.value

  tags = merge(var.tags, { Name = "${local.name_prefix}-${each.value}" })
}

resource "aws_sqs_queue" "dlq" {
  for_each = local.queues

  name                      = "${each.key}-dlq"
  message_retention_seconds = 1209600 # 14 dias - tempo maximo para investigar mensagens na DLQ

  tags = merge(var.tags, { Name = "${local.name_prefix}-${each.key}-dlq" })
}

resource "aws_sqs_queue" "this" {
  for_each = local.queues

  name                       = each.key
  visibility_timeout_seconds = each.value.visibility_timeout
  message_retention_seconds  = 345600 # 4 dias

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = each.value.max_receive_count
  })

  tags = merge(var.tags, { Name = "${local.name_prefix}-${each.key}" })
}

resource "aws_sns_topic_subscription" "this" {
  for_each = { for s in local.all_subscriptions : "${s.topic}->${s.queue}" => s }

  topic_arn            = aws_sns_topic.this[each.value.topic].arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.this[each.value.queue].arn
  raw_message_delivery = true
}

# Policy de recurso na fila autorizando o(s) topico(s) SNS relevantes a publicar
# nela - sem isso, o SNS nao consegue entregar (paridade com o comportamento
# real da AWS, que o init-aws.sh já replica manualmente no LocalStack).
data "aws_iam_policy_document" "queue_policy" {
  for_each = local.queues

  statement {
    sid     = "AllowSnsPublish"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    resources = [aws_sqs_queue.this[each.key].arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [for topic in local.queue_topic_names[each.key] : aws_sns_topic.this[topic].arn]
    }
  }
}

resource "aws_sqs_queue_policy" "this" {
  for_each  = local.queues
  queue_url = aws_sqs_queue.this[each.key].id
  policy    = data.aws_iam_policy_document.queue_policy[each.key].json
}
