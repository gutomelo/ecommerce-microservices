# 0002. Mailpit para e-mail local e apenas log para SMS

**Status:** Aceita

## Contexto

O `notification-service` precisa enviar e-mail e SMS ao reagir aos eventos da Saga (ver `docs/saga/fluxo-saga.md`). Não há provedor real de e-mail/SMS disponível no ambiente local (nem seria desejável depender de um provedor externo real para rodar o projeto localmente via `docker compose up`, conforme exigido pela arquitetura).

## Decisão

- **E-mail:** usar **Mailpit** (`axllent/mailpit`) como servidor SMTP fake no `docker-compose.yml`, com UI web em `http://localhost:8025` para inspecionar visualmente os e-mails enviados durante o desenvolvimento/demonstração. O `notification-service` (Marco 11) configura `spring.mail.host=mailpit`/`port=1025` sem TLS/autenticação.
- **SMS:** apenas logado (log estruturado, nível INFO) pelo `notification-service` - não há simulação de um provedor de SMS real (custo/complexidade não justificados para um projeto de portfólio).

## Consequências

- **Fica mais fácil:** verificar visualmente o conteúdo dos e-mails de confirmação/cancelamento do pedido durante o desenvolvimento e em demonstrações, sem custo ou dependência de credenciais externas.
- **Fica mais difícil:** nada de relevante localmente; ao migrar para AWS real, troca-se Mailpit por Amazon SES (e-mail) e Amazon SNS (SMS) - a interface de `notification-service` para "enviar notificação" deve permanecer desacoplada do provedor concreto para que essa troca seja de configuração, não de código.

## Alternativas consideradas

- **MailHog**: alternativa equivalente ao Mailpit (mesmo propósito), mas sem manutenção ativa no momento desta decisão; Mailpit é o sucessor mais atualizado com a mesma proposta.
- **Provedor real de e-mail/SMS (ex.: SendGrid, Twilio) mesmo em ambiente local**: rejeitado por introduzir dependência de credenciais externas e custo para simplesmente rodar o projeto localmente, contrariando o objetivo de `docker compose up` funcionar sem configuração manual.
