variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "service_names" {
  description = "Nomes dos servicos (um repositorio ECR por servico), ex.: [\"gateway-service\", \"auth-service\", ...]"
  type        = list(string)
}

variable "image_retention_count" {
  description = "Quantidade de imagens com tag a manter por repositorio (as mais antigas alem disso sao expiradas)"
  type        = number
  default     = 10
}

variable "tags" {
  type    = map(string)
  default = {}
}
