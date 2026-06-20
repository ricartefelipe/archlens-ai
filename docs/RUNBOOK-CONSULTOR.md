# Runbook do Consultor — ArchLens AI

## Pré-requisitos

```bash
docker compose up -d
export ARCHLENS_STORAGE_BASE_PATH=/tmp/archlens/projects
mkdir -p /tmp/archlens/projects
./mvnw quarkus:dev
cd worker-ai && uvicorn app.main:app --port 8081  # se não usar worker no Docker
cd frontend && npm run dev
```

Alinhar **sempre** `ARCHLENS_STORAGE_BASE_PATH` (backend) = `STORAGE_BASE_PATH` (worker).

## Fluxo de entrega (10 dias típicos)

### Dia 0 — Kick-off

- [ ] NDA assinado
- [ ] Acesso ao repositório (ZIP ou clone)
- [ ] Tenant criado no Keycloak (prod) ou tenant dev
- [ ] Escopo fechado: quais módulos entram

### Dia 1 — Ingestão

1. Criar projeto no ArchLens
2. Upload ZIP
3. Aguardar status **READY** (ingestão concluída)
4. Verificar arquivos listados na UI

### Dia 2–3 — Análise

1. Disparar análise
2. Aguardar **COMPLETED**
3. Revisar riscos — **descartar falsos positivos**
4. Complementar achados manualmente se necessário
5. Validar ADRs gerados

### Dia 4–5 — RAG / perguntas

1. Usar chat para perguntas exploratórias
2. Documentar respostas relevantes para o relatório

### Dia 6–8 — Relatório

1. Export Markdown/JSON
2. Preencher [TEMPLATE-RELATORIO-DIAGNOSTICO.md](./TEMPLATE-RELATORIO-DIAGNOSTICO.md)
3. Priorizar matriz de riscos
4. Escrever sumário executivo (linguagem de negócio)

### Dia 9 — Revisão interna

- [ ] Nenhum risco fictício (fallback LLM desligado: `ARCHLENS_ANALYSIS_LLM_FALLBACK=false`)
- [ ] Evidências conferidas no código
- [ ] Plano de ação realista

### Dia 10 — Apresentação

- Entrega PDF/Markdown + sessão 1h com stakeholders
- Registrar feedback para case study anonimizado

## Troubleshooting

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| Projeto preso em INGESTING | Worker down ou path errado | Verificar logs worker + paths |
| 0 riscos na análise | Arquivos não no storage do worker | Alinhar volumes/paths |
| Chat genérico | Chunks vazios | Re-ingestão |
| Riscos "mock" | Fallback LLM ativo | Desligar fallback em prod |

## Custos de inferência (referência)

| Provider | Uso | Ordem de magnitude |
|----------|-----|-------------------|
| local | Dev/demo | R$ 0 |
| OpenAI embeddings + chat | Prod | ~R$ 5–50/diagnóstico* |
| Ollama self-hosted | Prod | infra própria |

*Estimativa — medir por projeto real.
