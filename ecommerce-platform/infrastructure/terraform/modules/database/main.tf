# Uma instancia RDS PostgreSQL isolada por servico - migracao direta dos 7
# containers postgres:16-alpine do docker-compose (mesmo principio de "banco
# exclusivo por servico", ver .claude/rules/banco-de-dados.md e
# docs/deployment/aws.md). Credenciais geradas e guardadas no Secrets Manager,
# nunca em texto puro.

locals {
  name_prefix = "${var.project}-${var.environment}"
}

resource "aws_db_subnet_group" "this" {
  name       = "${local.name_prefix}-db-subnets"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.tags, { Name = "${local.name_prefix}-db-subnets" })
}

resource "aws_security_group" "rds" {
  name_prefix = "${local.name_prefix}-rds-"
  description = "PostgreSQL - acesso apenas das tasks ECS"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Postgres a partir das tasks ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${local.name_prefix}-rds-sg" })

  lifecycle {
    create_before_destroy = true
  }
}

resource "random_password" "this" {
  for_each = var.databases

  length  = 32
  special = false # evita caracteres que exigem escaping em connection strings/JDBC URL
}

resource "aws_db_instance" "this" {
  for_each = var.databases

  identifier     = "${local.name_prefix}-${each.key}"
  engine         = "postgres"
  engine_version = var.engine_version

  instance_class    = var.instance_class
  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = each.value.db_name
  username = each.value.username
  password = random_password.this[each.key].result
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  multi_az                  = var.multi_az
  backup_retention_period   = var.backup_retention_days
  deletion_protection       = var.deletion_protection
  skip_final_snapshot       = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${local.name_prefix}-${each.key}-final"

  tags = merge(var.tags, { Name = "${local.name_prefix}-${each.key}", Service = "${each.key}-service" })
}

# Segredo por servico com tudo que a aplicacao precisa para conectar - o task
# definition (modules/ecs-service) injeta cada chave como variavel de ambiente
# via "secrets" (nunca em texto puro no container_definitions).
resource "aws_secretsmanager_secret" "this" {
  for_each = var.databases

  name        = "${local.name_prefix}/${each.key}/database"
  description = "Credenciais do PostgreSQL do ${each.key}-service"

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "this" {
  for_each = var.databases

  secret_id = aws_secretsmanager_secret.this[each.key].id
  secret_string = jsonencode({
    DB_HOST     = aws_db_instance.this[each.key].address
    DB_PORT     = tostring(aws_db_instance.this[each.key].port)
    DB_NAME     = each.value.db_name
    DB_USERNAME = each.value.username
    DB_PASSWORD = random_password.this[each.key].result
  })
}
