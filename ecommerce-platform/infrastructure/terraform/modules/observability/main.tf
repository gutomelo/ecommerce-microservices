# Prometheus + Grafana + Jaeger como servicos ECS Fargate normais (mesmas
# imagens do docker-compose.yml), com um ALB interno proprio - nao ficam
# atras do ALB publico do gateway-service. Sem var.admin_cidr_blocks
# preenchido, ninguem alcanca essas UIs (fail-safe): acesso planejado via
# VPN/bastion, nunca exposicao publica de ferramenta administrativa.
#
# Persistencia: Prometheus (TSDB) e Grafana (dashboards/usuarios) ganham um
# volume EFS cada - upgrade real sobre o docker-compose local, onde só o
# Grafana tem volume e o Prometheus perde historico a cada restart. Jaeger
# continua in-memory (mesmo comportamento local hoje - ver docs/deployment/aws.md
# para a alternativa gerenciada via AWS X-Ray).

locals {
  name_prefix = "${var.project}-${var.environment}-observability"
}

# ---------------------------------------------------------------------------
# EFS - persistencia do Prometheus e do Grafana
# ---------------------------------------------------------------------------

resource "aws_security_group" "efs" {
  name_prefix = "${local.name_prefix}-efs-"
  description = "EFS - acesso apenas das tasks ECS"
  vpc_id      = var.vpc_id

  ingress {
    description     = "NFS a partir das tasks ECS"
    from_port       = 2049
    to_port         = 2049
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${local.name_prefix}-efs-sg" })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_efs_file_system" "this" {
  encrypted = true

  tags = merge(var.tags, { Name = "${local.name_prefix}-efs" })
}

resource "aws_efs_mount_target" "this" {
  count           = length(var.private_subnet_ids)
  file_system_id  = aws_efs_file_system.this.id
  subnet_id       = var.private_subnet_ids[count.index]
  security_groups = [aws_security_group.efs.id]
}

resource "aws_efs_access_point" "prometheus" {
  file_system_id = aws_efs_file_system.this.id

  posix_user {
    uid = 65534 # "nobody", usuario padrao da imagem prom/prometheus
    gid = 65534
  }

  root_directory {
    path = "/prometheus"
    creation_info {
      owner_uid   = 65534
      owner_gid   = 65534
      permissions = "755"
    }
  }

  tags = merge(var.tags, { Name = "${local.name_prefix}-prometheus-ap" })
}

resource "aws_efs_access_point" "grafana" {
  file_system_id = aws_efs_file_system.this.id

  posix_user {
    uid = 472 # usuario "grafana" da imagem grafana/grafana
    gid = 472
  }

  root_directory {
    path = "/grafana"
    creation_info {
      owner_uid   = 472
      owner_gid   = 472
      permissions = "755"
    }
  }

  tags = merge(var.tags, { Name = "${local.name_prefix}-grafana-ap" })
}

# ---------------------------------------------------------------------------
# ALB interno - só acessivel dos CIDRs em var.admin_cidr_blocks
# ---------------------------------------------------------------------------

resource "aws_security_group" "internal_alb" {
  name_prefix = "${local.name_prefix}-alb-"
  description = "ALB interno das UIs de observabilidade (Prometheus/Grafana/Jaeger)"
  vpc_id      = var.vpc_id

  dynamic "ingress" {
    for_each = length(var.admin_cidr_blocks) > 0 ? [9090, 3000, 16686] : []
    content {
      description = "UI de observabilidade"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = var.admin_cidr_blocks
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, { Name = "${local.name_prefix}-alb-sg" })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "ecs_from_internal_alb" {
  type                     = "ingress"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "tcp"
  security_group_id        = var.ecs_security_group_id
  source_security_group_id = aws_security_group.internal_alb.id
  description              = "ALB interno de observabilidade para prometheus/grafana/jaeger"
}

resource "aws_lb" "internal" {
  name               = "${local.name_prefix}-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_alb.id]
  subnets            = var.private_subnet_ids

  tags = merge(var.tags, { Name = "${local.name_prefix}-alb" })
}

resource "aws_lb_target_group" "prometheus" {
  name        = "${local.name_prefix}-prometheus"
  port        = 9090
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path = "/-/healthy"
  }

  tags = var.tags
}

resource "aws_lb_target_group" "grafana" {
  name        = "${local.name_prefix}-grafana"
  port        = 3000
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path = "/api/health"
  }

  tags = var.tags
}

resource "aws_lb_target_group" "jaeger" {
  name        = "${local.name_prefix}-jaeger"
  port        = 16686
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path = "/"
  }

  tags = var.tags
}

resource "aws_lb_listener" "prometheus" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 9090
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.prometheus.arn
  }
}

resource "aws_lb_listener" "grafana" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 3000
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana.arn
  }
}

resource "aws_lb_listener" "jaeger" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 16686
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.jaeger.arn
  }
}

# ---------------------------------------------------------------------------
# Credencial de admin do Grafana (Secrets Manager, nunca texto puro)
# ---------------------------------------------------------------------------

resource "random_password" "grafana_admin" {
  length  = 24
  special = false
}

resource "aws_secretsmanager_secret" "grafana_admin" {
  name = "${var.project}/${var.environment}/grafana-admin-password"
  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "grafana_admin" {
  secret_id     = aws_secretsmanager_secret.grafana_admin.id
  secret_string = random_password.grafana_admin.result
}

# ---------------------------------------------------------------------------
# Servicos ECS
# ---------------------------------------------------------------------------

module "prometheus" {
  source = "../ecs-service"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  service_name   = "prometheus"
  image_uri      = var.image_uris.prometheus
  container_port = 9090
  cpu            = 256
  memory         = 512

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.observability_task_role_arn

  subnet_ids         = var.private_subnet_ids
  security_group_ids = [var.ecs_security_group_id]
  cluster_id         = var.cluster_id
  namespace_id       = var.namespace_id
  target_group_arn   = aws_lb_target_group.prometheus.arn

  efs_volume = {
    file_system_id  = aws_efs_file_system.this.id
    access_point_id = aws_efs_access_point.prometheus.id
    container_path  = "/prometheus"
  }

  tags = var.tags
}

module "grafana" {
  source = "../ecs-service"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  service_name   = "grafana"
  image_uri      = var.image_uris.grafana
  container_port = 3000
  cpu            = 256
  memory         = 512

  environment_variables = {
    GF_SECURITY_ADMIN_USER     = "admin"
    GF_AUTH_ANONYMOUS_ENABLED  = "true"
    GF_AUTH_ANONYMOUS_ORG_ROLE = "Viewer"
  }
  secrets = {
    GF_SECURITY_ADMIN_PASSWORD = aws_secretsmanager_secret.grafana_admin.arn
  }

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.observability_task_role_arn

  subnet_ids         = var.private_subnet_ids
  security_group_ids = [var.ecs_security_group_id]
  cluster_id         = var.cluster_id
  namespace_id       = var.namespace_id
  target_group_arn   = aws_lb_target_group.grafana.arn

  efs_volume = {
    file_system_id  = aws_efs_file_system.this.id
    access_point_id = aws_efs_access_point.grafana.id
    container_path  = "/var/lib/grafana"
  }

  tags = var.tags
}

module "jaeger" {
  source = "../ecs-service"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  service_name   = "jaeger"
  image_uri      = var.image_uris.jaeger
  container_port = 16686

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.observability_task_role_arn

  subnet_ids         = var.private_subnet_ids
  security_group_ids = [var.ecs_security_group_id]
  cluster_id         = var.cluster_id
  namespace_id       = var.namespace_id
  target_group_arn   = aws_lb_target_group.jaeger.arn

  tags = var.tags
}
