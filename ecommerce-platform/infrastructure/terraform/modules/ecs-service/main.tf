# Modulo generico: 1 task definition + 1 ECS service Fargate + 1 entrada de
# Cloud Map. Instanciado uma vez por container (os 9 microsservicos +
# prometheus/grafana/jaeger, ver environments/prod/main.tf e modules/observability).
# ALB é opcional (só o gateway-service e os 3 da observability o usam).

locals {
  name_prefix       = "${var.project}-${var.environment}-${var.service_name}"
  has_efs           = var.efs_volume != null
  has_load_balancer = var.target_group_arn != ""
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.project}-${var.environment}/${var.service_name}"
  retention_in_days = var.log_retention_days

  tags = merge(var.tags, { Name = "${local.name_prefix}-logs" })
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.task_role_arn

  dynamic "volume" {
    for_each = local.has_efs ? [var.efs_volume] : []
    content {
      name = "data"
      efs_volume_configuration {
        file_system_id     = volume.value.file_system_id
        transit_encryption = "ENABLED"
        authorization_config {
          access_point_id = volume.value.access_point_id
          iam             = "ENABLED"
        }
      }
    }
  }

  container_definitions = jsonencode([
    {
      name      = var.service_name
      image     = var.image_uri
      essential = true
      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]
      environment = [for k, v in var.environment_variables : { name = k, value = v }]
      secrets     = [for k, v in var.secrets : { name = k, valueFrom = v }]
      mountPoints = local.has_efs ? [
        {
          sourceVolume  = "data"
          containerPath = var.efs_volume.container_path
          readOnly      = false
        }
      ] : []
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.this.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = var.service_name
        }
      }
    }
  ])

  tags = merge(var.tags, { Name = local.name_prefix })
}

resource "aws_service_discovery_service" "this" {
  name = var.service_name

  dns_config {
    namespace_id = var.namespace_id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = var.tags
}

resource "aws_ecs_service" "this" {
  name            = var.service_name
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = var.security_group_ids
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.this.arn
  }

  dynamic "load_balancer" {
    for_each = local.has_load_balancer ? [1] : []
    content {
      target_group_arn = var.target_group_arn
      container_name   = var.service_name
      container_port   = var.container_port
    }
  }

  tags = merge(var.tags, { Name = local.name_prefix })
}
