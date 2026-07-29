terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # O bucket S3 e a tabela DynamoDB de lock precisam existir ANTES do primeiro
  # `terraform init` (bootstrap manual - ver "Bootstrap do state remoto" no
  # README.md deste diretorio). Preencher bucket/dynamodb_table/region via
  # `-backend-config=backend.hcl` (arquivo local, NAO versionado) ou flags de
  # linha de comando - nunca commitar o nome do bucket real de uma conta AWS.
  backend "s3" {
    key     = "prod/terraform.tfstate"
    encrypt = true
  }
}
