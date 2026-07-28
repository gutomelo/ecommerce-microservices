---
name: idioma-e-estilo
description: Convenção de idioma do projeto — código em inglês, documentação e comentários em pt-BR
---

# Idioma e estilo

- Código-fonte em **inglês americano**: nomes de pacotes, classes, interfaces, métodos, variáveis, campos de DTO/entidade, chaves de `application.yml`, nomes de branches, nomes de arquivos e pastas de código.
- Mensagens de log e de exceção (texto que vira parte do contrato observável do sistema) também em inglês — são "estrutura interna", não documentação.
- **Somente** os seguintes artefatos são escritos em português do Brasil: comentários explicativos no código (quando realmente necessários), Javadoc de contexto de negócio, arquivos em `docs/`, README de cada módulo/serviço, ADRs, e mensagens de commit/PR.
- Nunca traduza identificadores de eventos, tópicos SNS ou filas SQS para inglês nem para português de forma inconsistente: use exatamente os nomes de evento definidos em `platform-events` e no catálogo de eventos (`docs/events/catalogo-eventos.md`), que usam nomenclatura de negócio em português (ex.: `PedidoCriadoEvent`), pois foram assim definidos no domínio de negócio do projeto.
- Não crie sinônimos: se o catálogo de eventos e o `BaseEvent` já definem um nome, reutilize-o. Não crie uma segunda classe equivalente com outro nome.
