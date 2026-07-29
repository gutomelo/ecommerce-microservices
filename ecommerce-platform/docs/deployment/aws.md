# Notas de migração para AWS real

Este projeto roda 100% localmente via Docker Compose + LocalStack (ver [`docs/deployment/local.md`](local.md)). O objetivo do desenho é que a migração para AWS real seja principalmente **configuração**, não reescrita de código — cada ponto abaixo já foi isolado atrás de uma porta (`platform-messaging`, `platform-security`) ou de propriedades externalizadas justamente para isso.

> A infraestrutura de produção descrita abaixo já está implementada como Terraform real em [`infrastructure/terraform/`](../../infrastructure/terraform/) (VPC, ECS Fargate, RDS, SNS/SQS, ALB, Secrets Manager, observabilidade) — ver o [README daquele diretório](../../infrastructure/terraform/README.md) para como aplicar e as lacunas do lado da aplicação que ainda faltam (perfil `local` isolado no config-repo, troca do e-mail para SES).

## Mensageria (SNS/SQS)

- **Hoje:** `spring.cloud.aws.endpoint=http://localstack:4566`, credenciais fixas (`test`/`test`), tópicos e filas criados por [`infrastructure/localstack/init/init-aws.sh`](../../infrastructure/localstack/init/init-aws.sh) toda vez que o LocalStack sobe.
- **Migração:** remover `spring.cloud.aws.endpoint` (ou apontar para a região real); tópicos/filas/DLQs/subscriptions passam a ser provisionados via IaC equivalente ao script de init, aplicado uma única vez por ambiente (não a cada subida, como hoje) — já implementado em [`infrastructure/terraform/modules/messaging`](../../infrastructure/terraform/modules/messaging).
- Nenhum código muda: `EventPublisher`/`EventConsumer` (`platform-messaging`) já abstraem o SDK da AWS - só a configuração de endpoint/credenciais é diferente.

## Credenciais AWS

- **Hoje:** `spring.cloud.aws.credentials.access-key/secret-key` fixos em `infrastructure/config-repo/application.yml`.
- **Migração:** usar a credential chain padrão do SDK (ECS Task Role, uma por "papel de mensageria" - ver [`infrastructure/terraform/environments/prod/main.tf`](../../infrastructure/terraform/environments/prod/main.tf)), removendo `access-key`/`secret-key` da configuração inteiramente. Spring Cloud AWS já cai automaticamente para a default credential provider chain quando essas propriedades não estão presentes. **Atenção:** isso exige isolar as duas chaves de `application.yml` num profile `local` - ver a seção "Antes de apontar isso para uma conta AWS de verdade" do [README do Terraform](../../infrastructure/terraform/README.md).

## Banco de dados

- **Hoje:** 7 containers PostgreSQL (um por serviço), com volumes Docker locais.
- **Migração:** 7 instâncias **Amazon RDS PostgreSQL**, uma por serviço - mantém o princípio de "banco exclusivo por serviço" (ver [`.claude/rules/banco-de-dados.md`](../../.claude/rules/banco-de-dados.md)). Só troca `DB_HOST`/`DB_PORT`/credenciais (via Secrets Manager, não variável de ambiente em texto puro) - já implementado em [`infrastructure/terraform/modules/database`](../../infrastructure/terraform/modules/database).

## Configuração centralizada

- **Hoje:** `config-server` serve arquivos locais de [`infrastructure/config-repo/`](../../infrastructure/config-repo/) (profile `native`).
- **Migração:** apontar o `config-server` para um repositório Git real (profile `git` do Spring Cloud Config), ou substituir por **AWS Systems Manager Parameter Store**/**AWS AppConfig** conforme preferência operacional da equipe.

## Segredos (JWT, senhas de banco, etc.)

- **Hoje:** `JWT_SECRET` e senhas de banco como variável de ambiente/valor default em `application.yml`.
- **Migração:** **AWS Secrets Manager**, injetado como variável de ambiente no momento do deploy (ECS já suporta isso nativamente via `secrets` na task definition) - o código não muda, só de onde o valor de `platform.security.jwt.secret` vem. Já implementado (JWT secret + credenciais de cada banco + senha do Grafana, todos gerados pelo próprio Terraform, nunca em texto puro).

## Notificações (e-mail e SMS)

- **Hoje:** `notification-service` envia e-mail via **Mailpit** (SMTP fake) e SMS apenas via log (ver [ADR 0002](../decisions/0002-mailpit-para-email-local-e-log-para-sms.md)).
- **Migração:**
  - E-mail: trocar `spring.mail.host`/`port` pelo endpoint SMTP do **Amazon SES** (ou usar o SDK do SES diretamente) - a troca fica isolada em `JavaMailEmailSender` (`infrastructure/notification/`), já que `application/` depende só da porta `EmailSender`. A permissão IAM (`ses:SendEmail`/`ses:SendRawEmail`) já está provisionada no papel do `notification-service`; falta o código do `SesEmailSender` e verificar um domínio/identidade no console do SES.
  - SMS: implementar um `SmsSender` que chama **Amazon SNS** (mensagens SMS), substituindo `LoggingSmsSender` - mesma porta, nova implementação.

## Observabilidade

- **Hoje:** Jaeger local (traces) + Prometheus/Grafana local (métricas), via OTLP.
- **Migração:** o Terraform sobe as mesmas ferramentas (Prometheus/Grafana/Jaeger) como serviços ECS atrás de um ALB interno, com Prometheus e Grafana ganhando persistência via EFS - ver [`infrastructure/terraform/modules/observability`](../../infrastructure/terraform/modules/observability). Trocar por **AWS X-Ray**/**Amazon Managed Prometheus**/**Amazon Managed Grafana** continua sendo uma alternativa gerenciada válida (só mudaria endpoints de configuração), mas não é o que este Terraform implementa hoje.

## Deploy e orquestração

- **Hoje:** `docker compose up`, uma imagem Docker por serviço (`services/*/Dockerfile`, já testadas e buildadas no CI - ver [`.github/workflows/ci.yml`](../../../.github/workflows/ci.yml)).
- **Migração:** as mesmas imagens Docker são publicadas em **Amazon ECR** e deployadas em **ECS Fargate** - já implementado em [`infrastructure/terraform/`](../../infrastructure/terraform/) (ver o [README daquele diretório](../../infrastructure/terraform/README.md) para o passo a passo de `terraform apply` + build/push das imagens).
- CI/CD: o workflow atual builda e testa; falta adicionar um job de `docker push` para o ECR e um step de deploy (fora do escopo deste projeto de portfólio, mas é a próxima extensão natural).

## Gateway

- **Hoje:** `gateway-service` (Spring Cloud Gateway) roteia, valida JWT e aplica rate limiting.
- **Migração:** continua como está, rodando em ECS Fargate atrás de um Application Load Balancer público (único ponto de entrada, ver `infrastructure/terraform/modules/alb`) - substituir por **Amazon API Gateway** continua sendo uma alternativa de trade-off (menos código para manter, menos controle), não implementada aqui.
