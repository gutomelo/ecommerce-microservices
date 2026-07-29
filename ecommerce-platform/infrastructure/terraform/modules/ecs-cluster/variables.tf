variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "namespace_name" {
  description = "Namespace DNS privado do Cloud Map - servicos ficam acessiveis como <nome>.<namespace> (ex.: auth-service.ecommerce.local), mesmo padrao de hostname ja usado no docker-compose"
  type        = string
  default     = "ecommerce.local"
}

variable "tags" {
  type    = map(string)
  default = {}
}
