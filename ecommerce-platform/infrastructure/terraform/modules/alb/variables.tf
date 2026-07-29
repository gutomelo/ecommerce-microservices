variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "target_port" {
  description = "Porta do gateway-service (container port do target group)"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  type    = string
  default = "/actuator/health"
}

variable "domain_name" {
  description = "Dominio para o certificado ACM + registro Route53 (ex.: api.meudominio.com). Vazio = só HTTP, sem HTTPS/Route53 (util pra portfolio sem dominio proprio)."
  type        = string
  default     = ""
}

variable "route53_zone_id" {
  description = "Zone ID do Route53 (obrigatorio só se domain_name for informado)"
  type        = string
  default     = ""
}

variable "tags" {
  type    = map(string)
  default = {}
}
