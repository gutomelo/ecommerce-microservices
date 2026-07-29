# Terraform - infraestrutura de produção (AWS real)

Provisiona na AWS o equivalente ao `docker-compose.yml` local: os 9 microsserviços
em **ECS Fargate**, um **RDS PostgreSQL** por serviço, os 7 tópicos SNS + 4 filas
SQS/DLQ da Saga, e Prometheus/Grafana/Jaeger. Nenhum código dos serviços muda -
só a origem de configuração/credenciais (ver
[`docs/deployment/aws.md`](../../docs/deployment/aws.md), que este Terraform
implementa).

> Este projeto é de portfólio: o objetivo do Terraform é ser **validável**
> (`terraform validate`, `terraform plan`) e ler como infraestrutura real de
> produção - não pressupõe que exista uma conta AWS por trás para de fato
> rodar `terraform apply` e gastar dinheiro.

## Estrutura

```text
infrastructure/terraform/
├── modules/
│   ├── networking/     # VPC, subnets publicas/privadas, NAT, security groups
│   ├── ecr/             # 1 repositorio por servico (imagem Docker)
│   ├── messaging/       # 7 topicos SNS + 4 filas SQS/DLQ + subscriptions (fan-out da Saga)
│   ├── database/        # 1 RDS PostgreSQL por servico + Secrets Manager
│   ├── ecs-cluster/     # Cluster ECS Fargate + namespace Cloud Map (service discovery)
│   ├── ecs-service/     # Modulo generico: 1 task definition + 1 service (instanciado 12x)
│   ├── alb/             # ALB publico em frente ao gateway-service (+ HTTPS/Route53 opcional)
│   └── observability/   # Prometheus/Grafana/Jaeger + EFS (persistencia) + ALB interno
├── environments/
│   └── prod/            # Unico ambiente hoje - liga todos os modulos acima
└── docker/               # Dockerfiles/config "de producao" do Prometheus e do Grafana
```

Cada microsserviço do catálogo (`docs/events/catalogo-eventos.md`) vira uma
`aws_iam_role` com permissão exatamente do que ele publica/consome - `order-service`
só publica em `OrderCreated`/`OrderConfirmed`/`OrderCancelled` e só consome
`order-queue`, e assim por diante (least privilege, ver
[`.claude/rules/seguranca.md`](../../.claude/rules/seguranca.md)).

## Pré-requisitos

- Terraform >= 1.9, AWS CLI configurado com credenciais de uma conta AWS real.
- Imagens Docker dos 9 serviços já publicáveis (`services/*/Dockerfile`, já
  testadas no CI) e das duas imagens de observabilidade customizadas deste
  diretório (`docker/Dockerfile.prometheus`, `docker/Dockerfile.grafana`) -
  Jaeger usa a imagem pública `jaegertracing/jaeger:2.12.0` diretamente, sem ECR.

## Bootstrap do state remoto

O bucket S3 e a tabela DynamoDB de lock precisam existir **antes** do primeiro
`terraform init` (problema clássico de ovo-e-galinha do backend remoto) - crie
uma vez, manualmente:

```bash
aws s3api create-bucket --bucket ecommerce-platform-terraform-state --region us-east-1
aws s3api put-bucket-versioning --bucket ecommerce-platform-terraform-state \
  --versioning-configuration Status=Enabled
aws dynamodb create-table --table-name ecommerce-platform-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

Depois, crie `environments/prod/backend.hcl` (não versionado, já coberto pelo
`.gitignore`) com:

```hcl
bucket         = "ecommerce-platform-terraform-state"
region         = "us-east-1"
dynamodb_table = "ecommerce-platform-terraform-locks"
```

## Como aplicar

```bash
cd environments/prod
cp terraform.tfvars.example terraform.tfvars   # ajuste admin_cidr_blocks no minimo
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

Depois do primeiro `apply` (cria ECR vazio), publique as imagens e rode
`terraform apply` de novo (ou só `aws ecs update-service --force-new-deployment`)
para os serviços pegarem a imagem nova:

```bash
cd ../../../..   # volta para ecommerce-platform/
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

./mvnw clean package -DskipTests
for svc in gateway-service config-server auth-service customer-service product-service order-service inventory-service payment-service notification-service; do
  docker build -t <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/$svc:latest services/$svc
  docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/$svc:latest
done

docker build -f infrastructure/terraform/docker/Dockerfile.prometheus -t <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/prometheus:latest .
docker build -f infrastructure/terraform/docker/Dockerfile.grafana    -t <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/grafana:latest .
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/prometheus:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce-platform/grafana:latest
```

## Antes de apontar isso para uma conta AWS de verdade

Este Terraform provisiona a infraestrutura corretamente, mas há uma lacuna do
lado da **aplicação** que ele não resolve sozinho (fora do escopo de infra):

- **`spring.cloud.aws.endpoint` e credenciais fixas do LocalStack.**
  `infrastructure/config-repo/application.yml` hoje serve `http://localstack:4566`
  e `access-key/secret-key: test/test` para **todo mundo**, sempre - não existe
  ainda um profile "local" isolado. Antes de rodar contra AWS real, mova essas
  duas chaves para um novo `application-local.yml` (profile `local`) e adicione
  `SPRING_PROFILES_ACTIVE: local` no `docker-compose.yml`. Sem isso, os serviços
  em produção tentariam conectar em `localstack:4566`, que não existe fora do
  Compose. Este Terraform já injeta `SPRING_PROFILES_ACTIVE=prod` em toda task
  (harmless hoje, ativa a mitigação automaticamente no dia em que o arquivo for
  criado).
- **E-mail via Amazon SES.** A permissão IAM (`ses:SendEmail`/`ses:SendRawEmail`)
  já está no papel do `notification-service`, mas o código ainda usa
  `JavaMailEmailSender` (SMTP, como o Mailpit local). Falta trocar por um
  `SesEmailSender` via SDK, conforme já documentado em
  [`docs/deployment/aws.md`](../../docs/deployment/aws.md#notificações-e-mail-e-sms).
  Sem essa troca, `notification-service` sobe normalmente mas não envia e-mail
  de verdade em produção.
- **Domínio SES verificado.** Enviar e-mail via SES exige um domínio/identidade
  verificados no console (fora do que Terraform automatiza aqui) e sair do
  sandbox do SES para enviar para destinatários não verificados.

## Decisões e simplificações assumidas

- **ECS Fargate**, não EKS - encaixe direto com os `Dockerfile` que já existem
  por serviço, sem precisar de manifests/Helm adicionais (ver
  [ADR sobre esta escolha](../../docs/decisions/) se você acabou de gerar uma).
- **1 RDS por serviço** (7 instâncias `db.t4g.micro` por padrão) - mais fiel à
  regra "banco exclusivo por serviço" do que consolidar em menos instâncias;
  ajustável via `rds_instance_class`/`rds_multi_az`.
- **Observabilidade não fica atrás do ALB público.** Prometheus/Grafana/Jaeger
  sobem atrás de um **ALB interno**, só alcançável de dentro da VPC ou dos
  CIDRs em `admin_cidr_blocks` (VPN/bastion/seu IP) - nunca expostos à internet
  por padrão.
- **Jaeger continua in-memory** (mesmo comportamento do `docker-compose.yml`
  local - perde os traces a cada restart da task). Prometheus e Grafana ganham
  um volume EFS cada (upgrade real sobre o local, que só persiste o Grafana).
- **HTTPS/Route53 são opcionais** (`domain_name`/`route53_zone_id` vazios = só
  HTTP no ALB público) - não pressupõe que você tenha um domínio comprado.
- **`desired_count = 1`** por serviço (escala de demonstração, não de tráfego
  real de produção).

## Destruir tudo

```bash
cd environments/prod
terraform destroy
```

RDS e Secrets Manager, por padrão (`skip_final_snapshot = true`,
`deletion_protection = false`), são destruídos sem confirmação extra - troque
esses dois para `true` se este deixar de ser um ambiente de portfólio/demo.
