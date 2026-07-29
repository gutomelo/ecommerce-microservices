# Notas de migração para AWS real

Este projeto roda 100% localmente via Docker Compose + LocalStack (ver [`docs/deployment/local.md`](local.md)). O objetivo do desenho é que a migração para AWS real seja principalmente **configuração**, não reescrita de código — cada ponto abaixo já foi isolado atrás de uma porta (`platform-messaging`, `platform-security`) ou de propriedades externalizadas justamente para isso.

## Mensageria (SNS/SQS)

- **Hoje:** `spring.cloud.aws.endpoint=http://localstack:4566`, credenciais fixas (`test`/`test`), tópicos e filas criados por [`infrastructure/localstack/init/init-aws.sh`](../../infrastructure/localstack/init/init-aws.sh) toda vez que o LocalStack sobe.
- **Migração:** remover `spring.cloud.aws.endpoint` (ou apontar para a região real); tópicos/filas/DLQs/subscriptions passam a ser provisionados via IaC (Terraform/CloudFormation/CDK) equivalente ao script de init, aplicado uma única vez por ambiente (não a cada subida, como hoje).
- Nenhum código muda: `EventPublisher`/`EventConsumer` (`platform-messaging`) já abstraem o SDK da AWS - só a configuração de endpoint/credenciais é diferente.

## Credenciais AWS

- **Hoje:** `spring.cloud.aws.credentials.access-key/secret-key` fixos em `infrastructure/config-repo/application.yml`.
- **Migração:** usar a credential chain padrão do SDK (IAM role da task/pod - ECS Task Role ou IRSA no EKS), removendo `access-key`/`secret-key` da configuração inteiramente. Spring Cloud AWS já cai automaticamente para a default credential provider chain quando essas propriedades não estão presentes.

## Banco de dados

- **Hoje:** 7 containers PostgreSQL (um por serviço), com volumes Docker locais.
- **Migração:** 7 instâncias **Amazon RDS PostgreSQL** (ou Aurora PostgreSQL), uma por serviço - mantém o princípio de "banco exclusivo por serviço" (ver [`.claude/rules/banco-de-dados.md`](../../.claude/rules/banco-de-dados.md)). Só troca `DB_HOST`/`DB_PORT`/credenciais (via Secrets Manager, não variável de ambiente em texto puro).

## Configuração centralizada

- **Hoje:** `config-server` serve arquivos locais de [`infrastructure/config-repo/`](../../infrastructure/config-repo/) (profile `native`).
- **Migração:** apontar o `config-server` para um repositório Git real (profile `git` do Spring Cloud Config), ou substituir por **AWS Systems Manager Parameter Store**/**AWS AppConfig** conforme preferência operacional da equipe.

## Segredos (JWT, senhas de banco, etc.)

- **Hoje:** `JWT_SECRET` e senhas de banco como variável de ambiente/valor default em `application.yml`.
- **Migração:** **AWS Secrets Manager** (ou Parameter Store com criptografia KMS), injetado como variável de ambiente no momento do deploy (ECS/EKS já suportam isso nativamente) - o código não muda, só de onde o valor de `platform.security.jwt.secret` vem.

## Notificações (e-mail e SMS)

- **Hoje:** `notification-service` envia e-mail via **Mailpit** (SMTP fake) e SMS apenas via log (ver [ADR 0002](../decisions/0002-mailpit-para-email-local-e-log-para-sms.md)).
- **Migração:**
  - E-mail: trocar `spring.mail.host`/`port` pelo endpoint SMTP do **Amazon SES** (ou usar o SDK do SES diretamente) - a troca fica isolada em `JavaMailEmailSender` (`infrastructure/notification/`), já que `application/` depende só da porta `EmailSender`.
  - SMS: implementar um `SmsSender` que chama **Amazon SNS** (mensagens SMS), substituindo `LoggingSmsSender` - mesma porta, nova implementação.

## Observabilidade

- **Hoje:** Jaeger local (traces) + Prometheus/Grafana local (métricas), via OTLP.
- **Migração:** apontar o exporter OTLP (`management.otlp.tracing.endpoint`) para **AWS X-Ray** (via OTel Collector com exporter X-Ray) ou para um Grafana Cloud/Jaeger gerenciado; métricas Prometheus podem ir para **Amazon Managed Service for Prometheus** + **Amazon Managed Grafana**. Nenhum código muda - só endpoints de configuração.

## Deploy e orquestração

- **Hoje:** `docker compose up`, uma imagem Docker por serviço (`services/*/Dockerfile`, já testadas e buildadas no CI - ver [`.github/workflows/ci.yml`](../../../.github/workflows/ci.yml)).
- **Migração:** as mesmas imagens Docker são publicáveis em **Amazon ECR** e deployáveis em **ECS Fargate** (mais simples, sem gerenciar nós) ou **EKS** (mais controle). O `docker-compose.yml` vira o ponto de partida para as task definitions/manifests Kubernetes equivalentes.
- CI/CD: o workflow atual builda e testa; falta adicionar um job de `docker push` para o ECR e um step de deploy (fora do escopo deste projeto de portfólio, mas é a próxima extensão natural).

## Gateway

- **Hoje:** `gateway-service` (Spring Cloud Gateway) roteia, valida JWT e aplica rate limiting.
- **Migração:** pode continuar como está (rodando em ECS/EKS atrás de um Application Load Balancer), ou ser substituído por **Amazon API Gateway** se a equipe preferir um gateway totalmente gerenciado - decisão de trade-off entre controle (Spring Cloud Gateway, custo de manter) e operação (API Gateway, menos código para manter).
