output "gateway_url" {
  description = "Ponto de entrada publico da plataforma (equivalente a localhost:8080 local)"
  value       = module.alb.url
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "grafana_url" {
  description = "So acessivel de dentro da VPC ou de var.admin_cidr_blocks"
  value       = module.observability.grafana_url
}

output "prometheus_url" {
  value = module.observability.prometheus_url
}

output "jaeger_url" {
  value = module.observability.jaeger_url
}

output "grafana_admin_password_secret_arn" {
  value = module.observability.grafana_admin_password_secret_arn
}

output "db_endpoints" {
  value = module.database.db_endpoints
}

output "sns_topic_arns" {
  value = module.messaging.topic_arns
}
