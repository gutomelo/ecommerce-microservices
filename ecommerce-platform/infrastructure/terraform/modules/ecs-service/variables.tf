variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "service_name" {
  description = "Nome do servico (ex.: auth-service) - usado em nomes de recursos e no DNS do Cloud Map"
  type        = string
}

variable "image_uri" {
  description = "URI completa da imagem no ECR, com tag (ex.: <account>.dkr.ecr.<region>.amazonaws.com/ecommerce-platform/auth-service:latest)"
  type        = string
}

variable "container_port" {
  type = number
}

variable "cpu" {
  description = "vCPU da task Fargate, em unidades de 1024 (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "memory" {
  description = "Memoria da task Fargate em MB"
  type        = number
  default     = 512
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "environment_variables" {
  description = "Variaveis de ambiente em texto puro (nao-sensiveis) - mesmas chaves ja usadas no docker-compose.yml de cada servico"
  type        = map(string)
  default     = {}
}

variable "secrets" {
  description = "Variaveis de ambiente sensiveis - mapa nome-da-variavel -> valueFrom (ARN do Secrets Manager, opcionalmente com \":chave::\" para pegar só um campo do JSON)"
  type        = map(string)
  default     = {}
}

variable "execution_role_arn" {
  description = "IAM role que a ECS Agent assume para dar pull da imagem no ECR e ler os secrets (compartilhada por todos os servicos)"
  type        = string
}

variable "task_role_arn" {
  description = "IAM role que o processo da aplicacao assume em runtime (permissoes de SNS/SQS especificas do servico, ver environments/prod/main.tf). null = servico nao precisa de permissao AWS nenhuma (ex.: auth-service, gateway-service)"
  type        = string
  default     = null
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "cluster_id" {
  type = string
}

variable "namespace_id" {
  type = string
}

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "target_group_arn" {
  description = "ARN do target group do ALB a anexar a este servico; string vazia = sem ALB (comunicacao só via Cloud Map)"
  type        = string
  default     = ""
}

variable "health_check_path" {
  description = "Path do Actuator usado no healthcheck do container (informativo/consistencia; o healthcheck real do ALB fica no target group)"
  type        = string
  default     = "/actuator/health"
}

variable "efs_volume" {
  description = "Volume EFS opcional (usado por prometheus/grafana para persistencia) - null = sem volume"
  type = object({
    file_system_id  = string
    access_point_id = string
    container_path  = string
  })
  default = null
}

variable "tags" {
  type    = map(string)
  default = {}
}
