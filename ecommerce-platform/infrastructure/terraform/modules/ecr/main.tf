# Um repositorio ECR por servico (9 microsservicos + observabilidade, ver
# environments/prod/main.tf) - mesma imagem buildada localmente/no CI
# (services/*/Dockerfile) e publicada aqui em vez de rodar via docker-compose.

resource "aws_ecr_repository" "this" {
  for_each = toset(var.service_names)

  name                 = "${var.project}/${each.value}"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(var.tags, { Name = "${var.project}-${var.environment}-${each.value}" })
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Manter só as ${var.image_retention_count} imagens mais recentes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.image_retention_count
        }
        action = { type = "expire" }
      }
    ]
  })
}
