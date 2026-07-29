variable "project" {
  description = "Nome do projeto, usado em tags e nomes de recursos"
  type        = string
}

variable "environment" {
  description = "Nome do ambiente (ex.: prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "Bloco CIDR da VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "Quantidade de availability zones usadas (subnets publicas + privadas em cada uma)"
  type        = number
  default     = 2
}

variable "single_nat_gateway" {
  description = "Se true, cria um unico NAT Gateway compartilhado (mais barato); se false, um por AZ (mais resiliente)"
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags comuns aplicadas a todos os recursos do modulo"
  type        = map(string)
  default     = {}
}
