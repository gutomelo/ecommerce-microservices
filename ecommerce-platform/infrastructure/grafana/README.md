# infrastructure/grafana

Provisionamento automático do Grafana — datasources já configurados na subida, sem nenhum clique manual.

## O que faz

`docker-compose.yml` monta [`provisioning/`](provisioning/) inteiro em `/etc/grafana/provisioning` (somente leitura). Dois provisionadores:

- **[`provisioning/datasources/datasources.yml`](provisioning/datasources/datasources.yml)**: registra dois datasources automaticamente:
  - **Prometheus** (`http://prometheus:9090`, marcado como default) — métricas.
  - **Jaeger** (`http://jaeger:16686`) — traces.
- **[`provisioning/dashboards/dashboards.yml`](provisioning/dashboards/dashboards.yml)**: configura um *provider* de dashboards que lê qualquer arquivo JSON colocado em `provisioning/dashboards/` (verifica a cada 30s, `foldersFromFilesStructure: false`).

## Estado atual: sem dashboard pronto

Não há nenhum dashboard JSON neste diretório ainda — só o mecanismo de provisionamento, pronto para pegar qualquer `.json` de dashboard que seja adicionado em `provisioning/dashboards/`. Ao abrir o Grafana hoja, os dois datasources já aparecem prontos para uso, mas a exploração de métricas é manual (Explore → Prometheus/Jaeger) até um dashboard ser adicionado.

## Como acessar

```text
http://localhost:3000
usuario: admin
senha:   admin
```

## Como adicionar um dashboard

1. Monte ou exporte um dashboard JSON (do próprio Grafana: engrenagem → *JSON Model*, ou de [grafana.com/dashboards](https://grafana.com/grafana/dashboards/)).
2. Salve em `infrastructure/grafana/provisioning/dashboards/<nome>.json`.
3. Em até 30s (ou reiniciando o container) o Grafana o carrega automaticamente — nenhuma configuração adicional.

## Referências

- [`infrastructure/prometheus/README.md`](../prometheus/README.md) — o que o datasource Prometheus expõe.
- [`.claude/rules/observabilidade.md`](../../.claude/rules/observabilidade.md) — regras de observabilidade do projeto.
