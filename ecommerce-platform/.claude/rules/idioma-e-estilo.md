---
name: idioma-e-estilo
description: Convenção de idioma do projeto — código em inglês, documentação e comentários em pt-BR
---

# Idioma e estilo

- Código-fonte em **inglês americano**: nomes de pacotes, classes, interfaces, métodos, variáveis, campos de DTO/entidade, chaves de `application.yml`, nomes de branches, nomes de arquivos e pastas de código.
- Mensagens de log e de exceção (texto que vira parte do contrato observável do sistema) também em inglês — são "estrutura interna", não documentação.
- **Somente** os seguintes artefatos são escritos em português do Brasil: comentários explicativos no código (quando realmente necessários), Javadoc de contexto de negócio, arquivos em `docs/`, README de cada módulo/serviço, ADRs, e mensagens de commit/PR.
- Eventos, tópicos SNS e filas SQS seguem a mesma regra: nome de classe/tópico/fila em inglês (ex.: `OrderCreatedEvent`, tópico `OrderCreated`). O termo de negócio equivalente em português (ex.: "Pedido Criado") é linguagem ubíqua e só aparece em `docs/` e em comentários — nunca em identificadores de código. Isso evita "Portunglish" (misturar substantivo em português com sufixo em inglês, como `PedidoCriadoEvent`).
- O catálogo de eventos (`docs/events/catalogo-eventos.md`) mantém a rastreabilidade entre o nome de código em inglês e o termo de negócio em português.
- Não crie sinônimos: se o catálogo de eventos e o `BaseEvent` já definem um nome, reutilize-o. Não crie uma segunda classe equivalente com outro nome.
