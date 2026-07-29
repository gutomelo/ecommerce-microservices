variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "ecs_security_group_id" {
  description = "SG das tasks ECS - unica origem autorizada a acessar os bancos"
  type        = string
}

variable "databases" {
  description = "Um banco isolado por servico (nunca compartilhar schema/instancia - ver .claude/rules/banco-de-dados.md)"
  type = map(object({
    db_name  = string
    username = string
  }))
  default = {
    auth = {
      db_name  = "auth_db"
      username = "auth"
    }
    customer = {
      db_name  = "customer_db"
      username = "customer"
    }
    product = {
      db_name  = "product_db"
      username = "product"
    }
    order = {
      db_name  = "order_db"
      username = "order_service"
    }
    inventory = {
      db_name  = "inventory_db"
      username = "inventory"
    }
    payment = {
      db_name  = "payment_db"
      username = "payment"
    }
    notification = {
      db_name  = "notification_db"
      username = "notification"
    }
  }
}

variable "engine_version" {
  description = "Versao do PostgreSQL - mesma major usada localmente (postgres:16-alpine)"
  type        = string
  default     = "16"
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage" {
  type    = number
  default = 20
}

variable "multi_az" {
  description = "Alta disponibilidade real de producao; false por padrao para manter o custo baixo em portfolio/demo"
  type        = bool
  default     = false
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

variable "deletion_protection" {
  description = "Recomendado true em producao real; false por padrao para permitir destruir o ambiente de portfolio facilmente"
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
