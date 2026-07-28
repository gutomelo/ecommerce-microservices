---
name: novo-adr
description: Cria uma nova Architecture Decision Record (ADR) em docs/decisions seguindo o template e numeração do projeto. Use quando uma decisão arquitetural relevante for tomada ou quando o usuário pedir explicitamente para "registrar uma ADR" ou "documentar essa decisão".
---

# Criar uma nova ADR

## Passos

1. Liste os arquivos existentes em `docs/decisions/` e determine o próximo número sequencial de 4 dígitos (ex.: se o último é `0001-...md`, o novo é `0002-...md`).
2. Nomeie o arquivo como `NNNN-titulo-curto-em-kebab-case-em-portugues.md`.
3. Use exatamente esta estrutura (em português):

```markdown
# NNNN. Título da decisão

**Status:** Proposta | Aceita | Substituída por NNNN

## Contexto

Qual problema ou força motivou essa decisão? Referencie a regra/seção de CLAUDE.md ou de .claude/rules/ relevante, se houver.

## Decisão

O que foi decidido, de forma direta.

## Consequências

O que fica mais fácil, o que fica mais difícil, trade-offs aceitos.

## Alternativas consideradas

Alternativas avaliadas e por que foram descartadas.
```

4. Uma ADR nunca é editada retroativamente para mudar a decisão em si — se a decisão muda, crie uma nova ADR marcando a antiga como "Substituída por NNNN".
5. Se a decisão afeta uma regra em `.claude/rules/`, atualize a regra correspondente para refletir a decisão, referenciando a ADR pelo número.
