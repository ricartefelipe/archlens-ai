# Avaliação ponta a ponta — ArchLens AI

**Data:** junho/2026  
**Versão analisada:** 0.1.0-SNAPSHOT  
**Objetivo:** tornar o produto funcional e rentável como **consultoria empacotada de diagnóstico arquitetural**.

---

## 1. Sumário executivo

O ArchLens AI possui **base técnica sólida** (hexagonal, Quarkus, worker Python, RAG, análise estática, UI Next.js, CI). Antes desta entrega, o ciclo **upload → ingestão → análise → relatório exportável** estava **quebrado** na prática: arquivos não alimentavam o worker automaticamente, RAG/chat dependiam de ingest manual, multi-tenant era frágil e não havia entregável comercial.

**Maturidade estimada (antes → depois desta entrega):**

| Dimensão | Antes | Depois |
|----------|-------|--------|
| Arquitetura/código base | 65–70% | 75–80% |
| Integração E2E funcional | 25–35% | 55–65% |
| Segurança/multi-tenant | 20–30% | 45–55% |
| Produto comercial consultoria | 10–15% | 35–45% |

---

## 2. O que já estava implementado

- CRUD de projetos com coluna `tenant_id`
- Upload ZIP, classificação de arquivos (Java, SQL, Docker, OpenAPI, pipeline)
- Pipeline assíncrono de análise via RabbitMQ
- Worker com analyzers estáticos, chunking, embeddings (local/openai/ollama)
- Persistência de riscos, ADRs, perguntas RAG
- UI: projetos, upload, polling de análise, relatório, chat
- Keycloak realm + OIDC no backend (perfil prod)
- CI em 3 jobs (backend, frontend, worker)
- Liquibase + pgvector

---

## 3. Gaps críticos identificados

### 3.1 Integração (bloqueadores E2E)

| Gap | Impacto | Status |
|-----|---------|--------|
| Backend não chamava `/v1/ingest` após upload | RAG vazio, chat inútil | **Corrigido** — `IngestOrchestrationService` |
| Storage desacoplado backend/worker | Análise estática sem arquivos | **Documentado + `.env.example`** — path compartilhado |
| Worker opcional no compose (`profile: worker`) | Dev esquecia de subir worker | **Corrigido** — worker sobe por padrão |
| Fallback LLM local com riscos fictícios | Entrega falsa ao cliente | **Corrigido** — desligado em `prod` |

### 3.2 Segurança / multi-tenant

| Gap | Impacto | Status |
|-----|---------|--------|
| Reads sem validação de tenant (UUID enumeration) | Vazamento de dados | **Corrigido** — `findByIdAndTenantId` |
| Frontend sem JWT / login fake | Spoof de tenant | **Parcial** — middleware + Keycloak opcional |
| Sem RLS Postgres | Isolamento DB fraco | **Pendente** — evolução produção |

### 3.3 Comercial (consultoria)

| Gap | Impacto | Status |
|-----|---------|--------|
| Sem export de relatório | Não há entregável ao cliente | **Corrigido** — Markdown + JSON |
| Sem template de entrega | Processo não repetível | **Corrigido** — `docs/TEMPLATE-RELATORIO-DIAGNOSTICO.md` |
| Sem README comercial | Difícil vender serviço | **Corrigido** — `docs/README-COMERCIAL.md` |
| Sem billing/quotas | Não monetiza self-service | **Pendente** — ver roadmap |
| Sem PDF branded | Entrega executiva incompleta | **Pendente** — Markdown cobre MVP |

---

## 4. Correções implementadas nesta entrega

### Backend (Java)

1. **`IngestGateway` + `WorkerAiIngestAdapter`** — dispara ingestão no worker
2. **`IngestOrchestrationService`** — pós-upload: `INGESTING` → poll → `READY`/`FAILED`
3. **Isolamento tenant** — `findByIdAndTenantId`, `existsByIdAndTenantId` em projetos, arquivos, análises, ADRs
4. **`ReportExportService` + `ReportResource`** — `GET .../report?format=markdown|json`
5. **`ProjectNotReadyException`** — bloqueia análise se projeto não está `READY`/`UPLOADED`
6. **`archlens.analysis.llm-fallback-enabled=false`** em perfil prod
7. **Config** — `archlens.storage.base-path`, `archlens.ingest.async`

### Infra

1. **`docker-compose.yml`** — worker sempre ativo, volume `projectdata` compartilhado
2. **`.env.example`** — guia de paths alinhados backend/worker

### Frontend

1. **`middleware.ts`** — guarda de rota via cookie de sessão
2. **`auth.ts`** — JWT Keycloak opcional + cookie de tenant
3. **Login** — modo dev (tenant) ou Keycloak (email/senha)
4. **Export** — botões Markdown/JSON no relatório

### Documentação

- `docs/README-COMERCIAL.md` — posicionamento e oferta
- `docs/TEMPLATE-RELATORIO-DIAGNOSTICO.md` — entregável ao cliente
- `docs/RUNBOOK-CONSULTOR.md` — passo a passo da consultoria
- `docs/ROADMAP-PRODUTO.md` — evoluções restantes
- `docs/ENTREGA-COMPLETA.md` — checklist

---

## 5. Fluxo E2E atual (pós-correção)

```
1. Login (tenant ou Keycloak)
2. Criar projeto
3. Upload ZIP → extração → INGESTING
4. Worker indexa chunks (embeddings) → READY
5. Disparar análise → RabbitMQ → worker analyze → riscos + ADRs
6. Chat RAG (contexto dos chunks)
7. Exportar relatório Markdown/JSON para entrega ao cliente
```

**Pré-requisitos operacionais:**

- `docker compose up -d` (postgres, rabbitmq, worker, keycloak)
- Backend com `ARCHLENS_STORAGE_BASE_PATH` = mesmo path que worker (`/tmp/archlens/projects` no host)
- RabbitMQ acessível na porta 5672

---

## 6. Roadmap para produto rentável (próximas entregas)

### Prioridade alta (30 dias)

1. **PDF com branding** — conversão do Markdown (Puppeteer ou biblioteca Java)
2. **OIDC E2E testado** — fluxo PKCE no frontend, remover login por tenant em prod
3. **RLS Postgres** — políticas por `tenant_id`
4. **Testes E2E** — Playwright: upload → READY → análise → export
5. **Demo gravada** — vídeo 3 min para prospecção

### Prioridade média (60–90 dias)

6. **Billing** — Stripe: pay-per-diagnóstico ou pacotes
7. **Quotas** — limite de uploads/análises/tokens por tenant
8. **Portal org** — convites, roles, auditoria
9. **Containerizar backend + frontend** no compose/K8s
10. **Mais analyzers** — .NET, Terraform, K8s manifests

### Prioridade baixa / SaaS (se mudar estratégia)

11. Self-service onboarding
12. Multi-região
13. White-label completo

---

## 7. Modelo de monetização recomendado

**Consultoria empacotada** (não SaaS self-service):

| Oferta | Faixa sugerida |
|--------|----------------|
| Diagnóstico 1 sistema | R$ 8.000 – R$ 18.000 |
| Auditoria portfólio | R$ 25.000 – R$ 40.000 |
| Due diligence M&A | R$ 30.000 – R$ 60.000 |

A ferramenta **acelera** a entrega; o cliente paga pela **leitura sênior** e pelo relatório executivo.

---

## 8. Conclusão

O ArchLens AI deixou de ser apenas vitrine técnica e passou a **fechar o ciclo mínimo vendável** de consultoria: ingestão automática, análise com evidências, export de relatório e isolamento tenant básico. Para operação comercial plena, faltam PDF branded, OIDC end-to-end em produção, billing e testes E2E — mapeados no roadmap.

**Próximo passo comercial:** usar o template de relatório em um piloto simbólico e gerar case anonimizado para prospecção.
