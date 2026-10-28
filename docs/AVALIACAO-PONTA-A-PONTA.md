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
| Sem RLS Postgres | Isolamento DB fraco | **Corrigido** — migrations 005 + 008 (tabelas de plataforma) |

### 3.3 Comercial (consultoria)

| Gap | Impacto | Status |
|-----|---------|--------|
| Sem export de relatório | Não há entregável ao cliente | **Corrigido** — Markdown + JSON |
| Sem template de entrega | Processo não repetível | **Corrigido** — `docs/TEMPLATE-RELATORIO-DIAGNOSTICO.md` |
| Sem README comercial | Difícil vender serviço | **Corrigido** — `docs/README-COMERCIAL.md` |
| Sem billing/quotas | Não monetiza self-service | **Corrigido** — quotas + planos (consultoria) |
| Sem PDF branded | Entrega executiva incompleta | **Corrigido** — PDF + logo + export comparativo |

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

## 6. Estado atual (jun/2026 — pós Fases 3–4)

| Dimensão | Maturidade |
|----------|------------|
| Arquitetura/código base | ~85% |
| Integração E2E funcional | ~75% |
| Segurança/multi-tenant | ~80% (RLS, OIDC roles, API keys) |
| Produto comercial consultoria | ~75% |
| Plataforma (org, webhooks, white-label) | ~70% |

**Entregue desde a avaliação inicial:** PDF branded, OIDC PKCE, RLS, E2E, Docker, quotas/planos, comparativo before/after, export comparativo, analyzers (Java, SQL, Docker, OpenAPI, pipeline, Terraform, K8s, .NET, Python, Go, TypeScript), portal org, API keys, webhooks, UI admin, runbook produção.

**Adiado (estratégia consultoria):** Stripe, landing integrada, dashboard custo IA.

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

O ArchLens AI cobre o **ciclo completo de consultoria empacotada**: ingestão, análise multi-stack (Java, SQL, Docker, OpenAPI, pipeline, Terraform, K8s, .NET, Python, Go, TypeScript), RAG/chat, relatório PDF branded, comparativo before/after com export, quotas comerciais, portal org, API pública e webhooks.

**Próximo passo comercial:** piloto pago com cliente real usando runbook consultor + case anonimizado. Evoluções opcionais: Stripe (piloto self-service), landing integrada, dashboard de custo IA.

---

## 9. Fechamento V4 — 28/10/2026

Reexecução local completa dos gates da CI sobre a branch `feature/melhorias-refactor`,
mais correção dos defeitos encontrados ao exercitar a stack real (Compose) com
autenticação Keycloak, upload sintético, análise, comparação e exportação.

### Gates executados

| Gate | Comando | Resultado |
|------|---------|-----------|
| Backend | `./mvnw -B verify` (Temurin 21, Testcontainers) | 88 testes, 0 falhas, 0 skips |
| Frontend lint | `npm run lint` | 0 erros, 0 warnings |
| Frontend tipos | `npx tsc --noEmit` | limpo |
| Frontend testes | `npm test` (Vitest) | 21/21 |
| Frontend build | `npm run build` (Next 16.4.3) | ok |
| Worker | `pytest` (Python 3.12 + Postgres 16, paridade CI) | 74/74, 0 skips |
| E2E mockado | Playwright, `retries=0` | 13/13 |
| Compose integrado | stack isolada + seed + jornada autenticada | ver abaixo |

### Defeitos corrigidos nesta execução

| # | Defeito | Prova |
|---|---------|-------|
| 1 | `archlens.llm.allow-simulated-demo` não mapeada no `@ConfigMapping` derrubava o boot (SRCFG00050) | `ArchLensLlmConfig.allowSimulatedDemo()`; `./mvnw verify` verde |
| 2 | 9 warnings de lint React (setState em efeito, deps, navegação) | hook `useClientValue`; `npm run lint` com 0 problemas |
| 3 | **Login pela UI quebrado no Compose entregue**: a UI chama a API na mesma origem, mas só o `docker-compose.prod.yml` tem nginx roteando `/v1`,`/public`,`/q`. Sem proxy, o POST de login caía no proxy de páginas (307) e virava 405 | rewrites no `next.config.ts` + `isApiPath` no `proxy.ts`; `proxy.test.ts`; login real retorna 200 na stack |
| 4 | **Bomba de descompressão**: ZIP de 120 KB escrevia 121 MB em disco e respondia 200 | limites de total/entradas/razão no `UploadService`; agora 413 com 8 KB escritos; `UploadServiceTest` + `UploadLimitsResourceTest` |
| 5 | Exceção de limite virava 500 porque o resource reembrulhava em `RuntimeException` | `UploadResource` repassa exceções de domínio; IT assere 413 |
| 6 | **Arquivos de credencial eram indexados** (`.tfvars`, `credentials.json`, `secrets.yml`) e iriam a embeddings/LLM | `FileClassifier.isSensitive` + `file_filter.py` no worker; upload de teste extraiu só os 2 arquivos legítimos e o índice ficou com 0 segredos |
| 7 | Worker aceitava `file_paths` com `../`, podendo ler arquivo de outro projeto/tenant | `resolve_within`; tentativa real registrou `requested=3, total=0, skipped=3` |
| 8 | Reentrega de mensagem após conclusão duplicaria riscos, quota e webhook; tenant do evento não era revalidado | `AnalysisConsumer.tenantMatches` / `isAlreadySettled` + testes |
| 9 | RAG respondia sem evidência e não detectava citação inventada | `AnswerGrounding` (abstenção, citações, anti-injeção) + corpus de avaliação |
| 10 | `%prod` fixava `llm-fallback-enabled: false`, tornando `ARCHLENS_ANALYSIS_LLM_FALLBACK` inócua e o guard de boot sem efeito | `application.yml` usa o mesmo placeholder nos dois perfis |
| 11 | Keycloak não subia em volume novo no Compose local (schema/banco ausente) | `docker-compose.yml` monta `infra/postgres/init` e aponta para o banco `keycloak` |

### Provas da jornada integrada (stack isolada `archlens-v4`)

- Login real via Keycloak (`/public/auth/login` → 200 com JWT contendo `tenant_id` e roles).
- Upload sintético (`samples/demo-loja`), ingestão, análise `COMPLETED` com 5 riscos
  e 6 arquivos, segunda análise, comparação `200`, comparação inexistente `404`,
  exportação PDF `200` (4 páginas).
- Isolamento: `tenant-2` recebe `404` na análise de `tenant-1` **mesmo forjando**
  `X-Tenant-Id: tenant-1`; sem token, `401`; exportação cross-tenant `404`;
  `GET /v1/projects` de `tenant-2` retorna `[]`.
- Quotas reais: `402` ao exceder análises do plano Piloto e projetos do Diagnóstico.

### Decisões registradas

- **Simulação vs. real:** `LocalLlmGateway.performsExternalInference() == false`; o
  resumo de fallback recebe prefixo de origem e `SimulatedGatewayStartupGuard`
  recusa boot de produção com fallback simulado sem `ARCHLENS_LLM_ALLOW_SIMULATED_DEMO=true`.
- **MISSING_TEST_COVERAGE:** contrato em `PipelineAnalyzer` — dispara quando um
  pipeline real (não placeholder) não contém etapa de testes; coberto por
  `test_pipeline_analyzer_flags_missing_stages` e `test_pipeline_analyzer_ignores_placeholder_sample`.
- **Sync main/develop:** workflow passou a ser somente leitura (sem PAT, sem PR
  automático, sem auto-merge). **Ação do proprietário:** desativar nas Actions a
  versão antiga já publicada do workflow em outras refs — editar o YAML na feature
  não impede que outras branches a disparem.

### Pendências registradas (P2)

- Plano comercial inválido em `PUT /v1/admin/tenants/{id}/plan` responde 500; deveria ser 400.
- `POST /v1/ingest/{id}` apaga os chunks antes de validar a lista: uma chamada com
  caminhos inválidos zera o índice do projeto (worker não é exposto publicamente).
- Proveniência persistida da resposta (versão de prompt/modelo/embedding, custo,
  latência) exige migration própria; hoje fica em log e no texto da resposta.

---

## 10. Gates de recuperação e segurança de dependências — 28/10/2026

Gates do C7 executados após o fechamento da release 0.2.0.

### Backup e restore (banco novo, validado pela aplicação)

`pg_dump` da base semeada (448 KB) restaurado em banco **novo e vazio** (`restored`,
0 tabelas antes), com `ON_ERROR_STOP=1` e zero erros. A validação foi feita pela
API da aplicação restaurada, com efeitos externos desligados:

| Verificação | Antes | Restaurado |
|---|---|---|
| projects / analyses / risks | 1 / 1 / 5 | 1 / 1 / 5 |
| chunks / files / adrs | 21 / 7 / 3 | 21 / 7 / 3 |
| índices / políticas RLS | — | 31 / 13 |
| dimensão dos vetores | 1536 | 1536 |

Pela aplicação apontada ao banco restaurado: readiness OK, login real via Keycloak,
projeto `demo-loja` legível (status READY, 7 arquivos), análise COMPLETED com 5
riscos e 3 ADRs, relatório PDF de 4 páginas, e as permissões preservadas —
`tenant-2` recebe 404 na análise de `tenant-1` e requisição sem token recebe 401.

O Keycloak também subiu em **volume novo** sem intervenção manual, confirmando a
correção do init do Postgres.

### Scan de dependências

| Alvo | Antes | Depois |
|---|---|---|
| `npm audit` frontend (runtime e dev) | 0 | 0 |
| `pip-audit` worker (runtime e dev) | 7 achados em `starlette 0.38.6` | "No known vulnerabilities found" |

Correção: `fastapi 0.115.0 → 0.162.0` e `uvicorn 0.32.0 → 0.54.0`, que resolvem
`starlette 1.10.0`. Validado com 74/74 testes do worker.

### Scan das imagens finais

Trivy 0.76.1, DB de 2026-10-28, severidades HIGH e CRITICAL, mesmo Dockerfile e
plataforma do gate.

| Imagem | Antes | Depois | O que era |
|---|---|---|---|
| backend | 4 HIGH | ver abaixo | Go stdlib do `/usr/bin/pebble` da base em cache antiga |
| frontend | 11 HIGH | **0** | todas no npm CLI embutido (`pacote`, `sigstore`, `brace-expansion`…), não no código |
| worker | 3 HIGH + 44 HIGH | **0** | `starlette` e 44 pacotes Debian **sem correção upstream** |

Correções aplicadas:

- **Bases fixadas por digest** (manifest list, cobrindo amd64 e arm64) nos três
  Dockerfiles, com instrução de atualização. O Dependabot passou a acompanhar o
  ecossistema `docker`, pois um digest fixo não recebe correção sozinho.
- **Worker migrado de `python:3.12-slim` para `python:3.12-alpine`**: as 44
  vulnerabilidades HIGH do Debian 13.7 não tinham `FixedVersion` publicada, e a
  base Alpine entrega a mesma stack com zero achados. `libpq-dev` deixou de ser
  necessário porque `psycopg[binary]` embute a libpq. Validado com a jornada real
  (upload → ingestão → análise concluída com 5 riscos).
- **npm removido da imagem final do frontend**: o runtime executa apenas
  `node server.js`, e o CLI carregava as únicas vulnerabilidades HIGH da imagem.
  Readiness e landing conferidas depois da remoção.
- **Quarkus 3.35.2 → 3.39.5**, corrigindo `quarkus-vertx-http` e arrastando
  netty, jackson, guava, micrometer, amqp-client e o driver Postgres atualizados —
  correção na raiz do grafo, em vez de 14 overrides individuais.

Um scan em amd64 não comprova arm64: ao anunciar outra plataforma, escaneie a
imagem construída para ela.
