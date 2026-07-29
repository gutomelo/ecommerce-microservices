# Diagrama de contexto (C4 - Nível 1)

Visão de mais alto nível: quem usa a plataforma e com quais sistemas externos ela se comunica. Detalhes internos (microsserviços, banco, mensageria) ficam no [diagrama de contêineres](c4-containers.md).

```mermaid
flowchart TB
    customer(["<b>Cliente</b><br/><i>Pessoa</i><br/>Cria pedidos, consulta<br/>produtos e estoque"])
    admin(["<b>Administrador</b><br/><i>Pessoa</i><br/>Gerencia catálogo,<br/>clientes e pedidos"])

    platform["<b>ecommerce-platform</b><br/><i>Sistema de software</i><br/>Backend de e-commerce orientado a eventos,<br/>coordenado por Saga por Coreografia"]

    mailbox(["<b>Caixa de e-mail do cliente</b><br/><i>Sistema externo</i><br/>Mailpit local em dev/demo;<br/>Amazon SES em produção real"])

    customer -->|"HTTPS/JSON, via Gateway<br/>(JWT Bearer token)"| platform
    admin -->|"HTTPS/JSON, via Gateway<br/>(JWT Bearer token, role ADMIN)"| platform
    platform -->|"Notificação de pedido<br/>criado/confirmado/cancelado (SMTP)"| mailbox

    style platform fill:#1168bd,color:#fff,stroke:#0b4884
    style customer fill:#08427b,color:#fff,stroke:#052c52
    style admin fill:#08427b,color:#fff,stroke:#052c52
    style mailbox fill:#999999,color:#fff,stroke:#6b6b6b
```

## Leitura do diagrama

- **Cliente** e **Administrador** nunca falam diretamente com um microsserviço — toda entrada é via `gateway-service`, autenticada por JWT emitido pelo `auth-service`.
- O único sistema externo real é a caixa de e-mail do cliente. Localmente, quem recebe esse e-mail é o **Mailpit** (`http://localhost:8026`); em produção seria o provedor de e-mail real do cliente, com a plataforma falando com **Amazon SES** (ver [`docs/deployment/aws.md`](../deployment/aws.md)).
- SMS é apenas logado localmente (sem sistema externo real ainda - ver [ADR 0002](../decisions/0002-mailpit-para-email-local-e-log-para-sms.md)).
