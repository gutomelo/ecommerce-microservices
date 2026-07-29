output "topic_arns" {
  description = "Mapa nome-do-evento -> ARN do topico SNS (ex.: OrderCreated)"
  value       = { for name, topic in aws_sns_topic.this : name => topic.arn }
}

output "queue_arns" {
  description = "Mapa nome-da-fila -> ARN (ex.: order-queue)"
  value       = { for name, queue in aws_sqs_queue.this : name => queue.arn }
}

output "queue_urls" {
  value = { for name, queue in aws_sqs_queue.this : name => queue.id }
}

output "dlq_arns" {
  value = { for name, queue in aws_sqs_queue.dlq : name => queue.arn }
}
