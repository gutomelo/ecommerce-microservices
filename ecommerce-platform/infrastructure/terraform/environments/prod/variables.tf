variable "project" {
  type    = string
  default = "ecommerce-platform"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "image_tag" {
  description = "Tag das imagens publicadas no ECR (CI publica com o SHA do commit ou 'latest')"
  type        = string
  default     = "latest"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "az_count" {
  type    = number
  default = 2
}

variable "single_nat_gateway" {
  type    = bool
  default = true
}

variable "desired_count" {
  description = "Numero de tasks por servico - escala de portfolio/demo, nao de producao real com trafego"
  type        = number
  default     = 1
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "rds_multi_az" {
  type    = bool
  default = false
}

variable "domain_name" {
  description = "Dominio do ALB publico (ex.: api.meudominio.com). Vazio = só HTTP, sem Route53/ACM."
  type        = string
  default     = ""
}

variable "route53_zone_id" {
  description = "Obrigatorio só se domain_name for informado"
  type        = string
  default     = ""
}

variable "admin_cidr_blocks" {
  description = "CIDRs autorizados a acessar Prometheus/Grafana/Jaeger (ex.: [\"203.0.113.4/32\"] - seu IP, ou o CIDR da VPN). Vazio = observabilidade inacessivel (fail-safe)."
  type        = list(string)
  default     = []
}
