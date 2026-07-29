# Ambiente de producao real na AWS - equivalente ao docker-compose.yml local,
# so que com ECS Fargate + RDS + SNS/SQS reais em vez de containers + LocalStack.
# Nenhum codigo de servico muda (ver docs/deployment/aws.md); só a origem da
# configuracao/credenciais.

locals {
  name_prefix = "${var.project}-${var.environment}"

  # ---------------------------------------------------------------------------
  # Cada um dos 9 microsservicos: porta, banco (quando tem) e o "papel de
  # mensageria" (quando publica/consome SNS/SQS) - ver docs/events/catalogo-eventos.md.
  # ---------------------------------------------------------------------------
  microservices = {
    "gateway-service" = {
      port           = 8080
      db_key         = null
      messaging_role = null
      cpu            = 512
      memory         = 1024
    }
    "config-server" = {
      port           = 8888
      db_key         = null
      messaging_role = null
      cpu            = 256
      memory         = 512
    }
    "auth-service" = {
      port           = 8081
      db_key         = "auth"
      messaging_role = null
      cpu            = 256
      memory         = 512
    }
    "customer-service" = {
      port           = 8082
      db_key         = "customer"
      messaging_role = null
      cpu            = 256
      memory         = 512
    }
    "product-service" = {
      port           = 8083
      db_key         = "product"
      messaging_role = null
      cpu            = 256
      memory         = 512
    }
    "order-service" = {
      port           = 8084
      db_key         = "order"
      messaging_role = "order"
      cpu            = 256
      memory         = 512
    }
    "inventory-service" = {
      port           = 8085
      db_key         = "inventory"
      messaging_role = "inventory"
      cpu            = 256
      memory         = 512
    }
    "payment-service" = {
      port           = 8086
      db_key         = "payment"
      messaging_role = "payment"
      cpu            = 256
      memory         = 512
    }
    "notification-service" = {
      port           = 8087
      db_key         = "notification"
      messaging_role = "notification"
      cpu            = 256
      memory         = 512
    }
  }

  # Quem publica em quais topicos e de qual fila consome, por "papel de
  # mensageria" (replica 1:1 a coluna "Publica"/"Escuta" do catalogo de eventos).
  messaging_permissions = {
    order = {
      publish_topics = ["OrderCreated", "OrderConfirmed", "OrderCancelled"]
      consume_queue  = "order-queue"
    }
    inventory = {
      publish_topics = ["StockReserved", "StockUnavailable"]
      consume_queue  = "inventory-queue"
    }
    payment = {
      publish_topics = ["PaymentApproved", "PaymentDeclined"]
      consume_queue  = "payment-queue"
    }
    notification = {
      publish_topics = []
      consume_queue  = "notification-queue"
    }
  }

  namespace_name = module.ecs_cluster.namespace_name

  # URL do config-server e do jaeger via Cloud Map (substituem os hostnames
  # "config-server"/"jaeger" da rede docker-compose local).
  config_server_url    = "http://config-server.${local.namespace_name}:8888"
  otlp_traces_endpoint = "http://jaeger.${local.namespace_name}:4318/v1/traces"

  common_environment_variables = {
    CONFIG_SERVER_URL                = local.config_server_url
    SPRING_PROFILES_ACTIVE           = var.environment
    MANAGEMENT_OTLP_TRACING_ENDPOINT = local.otlp_traces_endpoint
  }

  gateway_environment_variables = {
    AUTH_SERVICE_URL      = "http://auth-service.${local.namespace_name}:8081"
    CUSTOMER_SERVICE_URL  = "http://customer-service.${local.namespace_name}:8082"
    PRODUCT_SERVICE_URL   = "http://product-service.${local.namespace_name}:8083"
    ORDER_SERVICE_URL     = "http://order-service.${local.namespace_name}:8084"
    INVENTORY_SERVICE_URL = "http://inventory-service.${local.namespace_name}:8085"
    PAYMENT_SERVICE_URL   = "http://payment-service.${local.namespace_name}:8086"
  }

  ecr_service_names = concat(keys(local.microservices), ["prometheus", "grafana"])
}

# ---------------------------------------------------------------------------
# Rede, registro de imagens, mensageria, bancos
# ---------------------------------------------------------------------------

module "networking" {
  source = "../../modules/networking"

  project            = var.project
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  az_count           = var.az_count
  single_nat_gateway = var.single_nat_gateway
}

module "ecr" {
  source = "../../modules/ecr"

  project       = var.project
  environment   = var.environment
  service_names = local.ecr_service_names
}

module "messaging" {
  source = "../../modules/messaging"

  project     = var.project
  environment = var.environment
}

module "database" {
  source = "../../modules/database"

  project               = var.project
  environment           = var.environment
  vpc_id                = module.networking.vpc_id
  private_subnet_ids    = module.networking.private_subnet_ids
  ecs_security_group_id = module.networking.ecs_security_group_id
  instance_class        = var.rds_instance_class
  multi_az              = var.rds_multi_az
}

module "ecs_cluster" {
  source = "../../modules/ecs-cluster"

  project     = var.project
  environment = var.environment
  vpc_id      = module.networking.vpc_id
}

module "alb" {
  source = "../../modules/alb"

  project           = var.project
  environment       = var.environment
  vpc_id            = module.networking.vpc_id
  public_subnet_ids = module.networking.public_subnet_ids
  security_group_id = module.networking.alb_security_group_id
  target_port       = local.microservices["gateway-service"].port
  domain_name       = var.domain_name
  route53_zone_id   = var.route53_zone_id
}

# ---------------------------------------------------------------------------
# IAM - execution role compartilhada (pull ECR + ler secrets + logs) e uma
# task role por "papel de mensageria" (least privilege: só order/inventory/
# payment/notification tocam SNS/SQS - ver .claude/rules/seguranca.md).
# ---------------------------------------------------------------------------

data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${local.name_prefix}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "execution_secrets" {
  statement {
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = ["arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.project}/${var.environment}/*"]
  }
}

resource "aws_iam_role_policy" "execution_secrets" {
  name   = "${local.name_prefix}-execution-secrets"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_secrets.json
}

resource "aws_iam_role" "messaging" {
  for_each = local.messaging_permissions

  name               = "${local.name_prefix}-${each.key}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

data "aws_iam_policy_document" "messaging" {
  for_each = local.messaging_permissions

  dynamic "statement" {
    for_each = length(each.value.publish_topics) > 0 ? [1] : []
    content {
      sid       = "Publish"
      effect    = "Allow"
      actions   = ["sns:Publish"]
      resources = [for topic in each.value.publish_topics : module.messaging.topic_arns[topic]]
    }
  }

  statement {
    sid    = "Consume"
    effect = "Allow"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:ChangeMessageVisibility",
    ]
    resources = [module.messaging.queue_arns[each.value.consume_queue]]
  }

  # notification-service: envio de e-mail real via Amazon SES (ver
  # docs/deployment/aws.md - substitui o Mailpit local). Requer trocar
  # JavaMailEmailSender por um SesEmailSender usando o SDK diretamente;
  # a permissao ja fica pronta aqui antes dessa troca de codigo acontecer.
  dynamic "statement" {
    for_each = each.key == "notification" ? [1] : []
    content {
      sid       = "SendEmail"
      effect    = "Allow"
      actions   = ["ses:SendEmail", "ses:SendRawEmail"]
      resources = ["*"]
    }
  }
}

resource "aws_iam_role_policy" "messaging" {
  for_each = local.messaging_permissions

  name   = "${local.name_prefix}-${each.key}-messaging"
  role   = aws_iam_role.messaging[each.key].id
  policy = data.aws_iam_policy_document.messaging[each.key].json
}

# ---------------------------------------------------------------------------
# JWT secret - compartilhado entre auth-service (emite) e os demais servicos
# que validam (platform-security), ver .claude/rules/seguranca.md.
# ---------------------------------------------------------------------------

resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "${var.project}/${var.environment}/jwt-secret"
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = random_password.jwt_secret.result
}

# ---------------------------------------------------------------------------
# Os 9 microsservicos - um modules/ecs-service por servico.
# ---------------------------------------------------------------------------

module "services" {
  source   = "../../modules/ecs-service"
  for_each = local.microservices

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  service_name   = each.key
  image_uri      = "${module.ecr.repository_urls[each.key]}:${var.image_tag}"
  container_port = each.value.port
  cpu            = each.value.cpu
  memory         = each.value.memory
  desired_count  = var.desired_count

  environment_variables = merge(
    local.common_environment_variables,
    each.key == "gateway-service" ? local.gateway_environment_variables : {},
  )

  secrets = merge(
    each.key != "config-server" ? { JWT_SECRET = aws_secretsmanager_secret.jwt_secret.arn } : {},
    each.value.db_key != null ? {
      DB_HOST     = "${module.database.secret_arns[each.value.db_key]}:DB_HOST::"
      DB_PORT     = "${module.database.secret_arns[each.value.db_key]}:DB_PORT::"
      DB_NAME     = "${module.database.secret_arns[each.value.db_key]}:DB_NAME::"
      DB_USERNAME = "${module.database.secret_arns[each.value.db_key]}:DB_USERNAME::"
      DB_PASSWORD = "${module.database.secret_arns[each.value.db_key]}:DB_PASSWORD::"
    } : {},
  )

  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = each.value.messaging_role != null ? aws_iam_role.messaging[each.value.messaging_role].arn : null

  subnet_ids         = module.networking.private_subnet_ids
  security_group_ids = [module.networking.ecs_security_group_id]
  cluster_id         = module.ecs_cluster.cluster_id
  namespace_id       = module.ecs_cluster.namespace_id

  target_group_arn = each.key == "gateway-service" ? module.alb.target_group_arn : ""
}

# ---------------------------------------------------------------------------
# Observabilidade
# ---------------------------------------------------------------------------

module "observability" {
  source = "../../modules/observability"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  vpc_id                = module.networking.vpc_id
  private_subnet_ids    = module.networking.private_subnet_ids
  ecs_security_group_id = module.networking.ecs_security_group_id
  cluster_id            = module.ecs_cluster.cluster_id
  namespace_id          = module.ecs_cluster.namespace_id
  execution_role_arn    = aws_iam_role.execution.arn
  admin_cidr_blocks     = var.admin_cidr_blocks

  image_uris = {
    prometheus = "${module.ecr.repository_urls["prometheus"]}:${var.image_tag}"
    grafana    = "${module.ecr.repository_urls["grafana"]}:${var.image_tag}"
    jaeger     = "jaegertracing/jaeger:2.12.0"
  }
}
