# Cluster ECS Fargate + namespace Cloud Map (service discovery DNS privado -
# substitui a rede Docker Compose "ecommerce-network": em vez de resolver
# "auth-service" pelo nome do container, resolve "auth-service.ecommerce.local").

resource "aws_ecs_cluster" "this" {
  name = "${var.project}-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(var.tags, { Name = "${var.project}-${var.environment}" })
}

resource "aws_service_discovery_private_dns_namespace" "this" {
  name        = var.namespace_name
  description = "Service discovery interno dos microsservicos do ${var.project} (${var.environment})"
  vpc         = var.vpc_id

  tags = var.tags
}
