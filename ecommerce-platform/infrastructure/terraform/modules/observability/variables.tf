variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "ecs_security_group_id" {
  type = string
}

variable "cluster_id" {
  type = string
}

variable "namespace_id" {
  type = string
}

variable "execution_role_arn" {
  type = string
}

variable "observability_task_role_arn" {
  description = "Task role sem permissoes especiais (prometheus/grafana/jaeger nao tocam SNS/SQS) - null = sem task role"
  type        = string
  default     = null
}

variable "image_uris" {
  description = "Mapa prometheus/grafana/jaeger -> URI completa da imagem no ECR"
  type = object({
    prometheus = string
    grafana    = string
    jaeger     = string
  })
}

variable "admin_cidr_blocks" {
  description = "CIDRs autorizados a acessar as UIs de Prometheus/Grafana/Jaeger (ex.: IP do escritorio/VPN). Vazio = ninguem acessa (fail-safe: nao expor observabilidade publicamente por padrao)."
  type        = list(string)
  default     = []
}

variable "tags" {
  type    = map(string)
  default = {}
}
