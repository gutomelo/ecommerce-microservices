# 0003. ECS Fargate para a infraestrutura de produção na AWS

**Status:** Aceita

## Contexto

O projeto rodava só localmente via Docker Compose + LocalStack (ver `docs/deployment/local.md`). `docs/deployment/aws.md` já descrevia, em texto, o que mudaria numa migração para AWS real - faltava a infraestrutura como código de fato. Criamos [`infrastructure/terraform/`](../../infrastructure/terraform/) para provisionar essa infraestrutura de produção real: VPC, RDS PostgreSQL por serviço, SNS/SQS replicando o catálogo de eventos (`docs/events/catalogo-eventos.md`), ALB, Secrets Manager, e os 9 microsserviços rodando em containers, mais Prometheus/Grafana/Jaeger.

A primeira decisão necessária antes de desenhar o Terraform foi qual plataforma de orquestração de containers usar - isso determina o formato de praticamente todo o resto (task definitions vs. manifests Kubernetes, service discovery, etc.).

## Decisão

Usar **ECS Fargate**, não EKS, para rodar os 9 microsserviços e os 3 componentes de observabilidade em produção.

ECS Fargate encaixa diretamente com os `Dockerfile` que cada serviço já tem (`services/*/Dockerfile`, já testados e buildados no CI) - vira só um repositório ECR + task definition + service por serviço, sem precisar de manifests Kubernetes/Helm charts adicionais, sem Ingress Controller, e sem gerenciar nós/cluster (serverless: a AWS cuida do plano de dados).

## Consequências

- **Fica mais fácil:** o Terraform fica menor e mais direto de revisar (um módulo `ecs-service` genérico reutilizado 12x, em vez de módulo de cluster + node groups + manifests/Helm por serviço); não há superfície operacional de gerenciar nós EC2 do cluster; o service discovery via AWS Cloud Map (DNS privado `<serviço>.ecommerce.local`) reproduz quase 1:1 os hostnames que os serviços já usam na rede `docker-compose` local, minimizando mudança de configuração.
- **Fica mais difícil:** menos controle fino de orquestração (scheduling, DaemonSets, sidecars nativos) do que EKS daria; para quem avalia o portfólio especificamente à procura de Kubernetes, ECS não demonstra essa competência - é um trade-off consciente de escopo, não uma limitação técnica do problema em si.

## Alternativas consideradas

- **EKS (Kubernetes gerenciado):** mais controle e mais frequentemente pedido em vagas de mercado, mas exigiria escopo bem maior - manifests/Helm chart por serviço, Ingress Controller, gerenciamento de node groups - equivalente a um projeto dentro do projeto. Rejeitado por desproporcional ao objetivo (demonstrar a arquitetura de microsserviços/Saga já implementada rodando em produção real, não uma segunda demonstração de operação Kubernetes).
- **EC2 direto, sem orquestrador:** rejeitado por perder de graça a elasticidade e o gerenciamento de ciclo de vida (restart automático, rolling deploy, health check integrado) que tanto ECS Fargate quanto EKS já resolvem.
