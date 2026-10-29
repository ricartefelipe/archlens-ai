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

As duas primeiras foram resolvidas na sequência — ver §11.

- ~~Plano comercial inválido em `PUT /v1/admin/tenants/{id}/plan` responde 500.~~ Corrigido.
- ~~`POST /v1/ingest/{id}` apaga os chunks antes de validar a lista.~~ Corrigido.
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


---

## 11. Erros de cliente e índice preservado — 28/10/2026

Fechamento das pendências P2 abertas em §10, mais um defeito que elas revelaram.

### Exceções de status viravam 500

`GenericExceptionMapper` implementa `ExceptionMapper<Exception>`, que em JAX-RS
também captura as exceções de status do próprio framework. O resultado: um
`ForbiddenException` lançado por `AdminTenantResource.updateStatus` chegava ao
cliente como **500**, indistinguível de falha do servidor, e o mesmo valia para
qualquer `NotFoundException` ou `BadRequestException` lançada em um resource.

O mapper passou a preservar o status e o corpo das `WebApplicationException`:
5xx continua com mensagem genérica e log de erro; 4xx devolve o status original
com a mensagem que o cliente precisa para corrigir a requisição. Coberto por
`AdminTenantResourceTest`, que assere 403 (não 500) nas três rotas de admin.

### Valor de enum inválido

`CommercialPlan.valueOf(...)` e `TenantAccountStatus.valueOf(...)` lançavam
`IllegalArgumentException` — erro do cliente apresentado como 500. Agora um
`parseEnum` comum responde **400** listando os valores aceitos, tolerando
diferença de caixa e espaços; `@Valid`, que faltava no endpoint de plano,
rejeita o campo ausente. Coberto por `AdminTenantResourceParseTest`.

### Ingestão apagava o índice antes de validar os caminhos

`IngestService.ingest` chamava `delete_by_project_id` **antes** de descobrir se
havia algo para indexar. Uma chamada com caminhos inválidos — travessia, arquivo
inexistente ou apenas arquivos sensíveis — zerava o índice do projeto e não
reindexava nada. Agora, sem caminho utilizável, o índice é preservado e a
resposta tem `status: "no_files"` com `total_chunks` informando o que continua
indexado. Coberto por `test_ingest_preserves_index.py` (3 casos: travessia,
só arquivos sensíveis, e reindexação legítima que continua trocando o índice).

### Dependabot apontava para `main`

Sem `target-branch`, o Dependabot abre PR contra a branch padrão: havia 18 PRs
abertos direto em `main`, contornando a integração em `develop` que o README
define. Todas as entradas passaram a declarar `target-branch: develop`.

---

## 12. Ciclo de vida da análise e erasure — 28/10/2026

Fechamento das lacunas levantadas na revisão de §11: o pipeline não tinha como
liberar trabalho abandonado, não havia cancelamento e não havia exclusão de dados.

### Análise abandonada ficava presa para sempre

Não existia lease, heartbeat, timeout nem tarefa periódica. O consumidor marcava
`PROCESSING` e, se o processo caísse ali sem reentrega da mensagem, a análise
permanecia nesse estado indefinidamente — o cliente via progresso eterno e só um
UPDATE manual no banco resolvia.

`StuckAnalysisSweeper` roda a cada `archlens.analysis.sweep-every` (padrão 5m) e
marca como `FAILED`, com mensagem legível, tudo que está em andamento sem progresso
há mais de `archlens.analysis.stuck-after` (padrão 30m). A varredura cruza tenants,
então usa o bypass de RLS apenas na sua transação, e notifica cada webhook no tenant
dono da análise. O prazo precisa ser maior que a análise legítima mais longa: expirar
cedo derrubaria trabalho vivo. A reentrega do broker continua sendo o caminho
primário de retomada; o varredor é a rede de segurança.

**Defeito encontrado no caminho:** `AnalysisRepositoryAdapter.save` não copiava
`updatedAt` no update, então a coluna ficava congelada no valor do insert e não
refletia transição alguma de status. Sem isso, nenhum varredor por idade
funcionaria.

### Cancelamento

`POST /v1/projects/{id}/analyses/{id}/cancel` transiciona para `CANCELLED` com
lock pessimista na linha, que serializa a corrida com o consumidor: quem pega o
lock primeiro decide. O consumidor relê o status antes de gravar `COMPLETED` ou
`FAILED` e descarta resultado tardio de análise cancelada — sem quota nem webhook
de conclusão. Cancelar o que já está cancelado é idempotente; estado terminal
responde 409. `isAlreadySettled` passou a incluir `CANCELLED`, para que uma
reentrega não ressuscite o que o usuário parou.

A operação devolve o estado resultante em vez de um booleano: a releitura após o
update lia a instância do contexto de persistência, ainda com o status antigo.

### Erasure

`DELETE /v1/projects/{id}` apaga, nesta ordem: índice e vetores no worker
(`DELETE /v1/ingest/{id}`, novo), arquivos extraídos no storage e as linhas
relacionais (cascata leva análises, riscos, ADRs, perguntas e arquivos). A ordem
importa — se o registro saísse primeiro e o worker falhasse, os embeddings ficariam
órfãos sem dono capaz de apagá-los. Cada passo é idempotente, então uma falha no
meio deixa o projeto visível e a operação pode ser repetida. Na UI, exige digitar
o nome do projeto para confirmar.

### Endpoints sem corpo exigiam Content-Type

`POST /analyses`, o novo `/cancel` e o `DELETE` de projeto herdavam
`@Consumes(APPLICATION_JSON)` da classe e respondiam **415** a um cliente que não
mandasse o header — o frontend sempre manda, um `curl` simples não. Passaram a
aceitar `MediaType.WILDCARD`.

---

## 13. Atualizações de dependência pendentes — 28/10/2026

Os 17 PRs que o Dependabot havia aberto **contra `main`** foram fechados: o fluxo
do projeto manda tudo entrar por `develop`, e a configuração agora declara
`target-branch: develop`. O Dependabot reabre o que continuar aplicável na próxima
execução semanal, já na branch certa.

Fechar um PR do Dependabot pode impedir que ele recrie aquela versão exata, então
a lista fica registrada aqui para conferência:

| Alvo | De → para | Observação |
|---|---|---|
| `actions/checkout` | 4 → 7 | major de action |
| `actions/setup-node` | 4 → 7 | major de action |
| `actions/setup-java` | 4 → 6 | major de action |
| `actions/setup-python` | 5 → 7 | major de action |
| `softprops/action-gh-release` | 2 → 3 | major de action |
| `@playwright/test` | 1.61.0 → 1.64.1 | minor, E2E |
| `pytest` | 9.0.3 → 9.1.1 | minor, worker |
| `python-dotenv` | 1.2.2 → 1.2.4 | patch, worker |
| `structlog` | 24.4.0 → 26.1.0 | major, worker |
| `pgvector` | 0.3.6 → 0.5.1 | minor, worker |
| `typescript` | 5.9.3 → 7.0.2 | major, dev |
| `eslint` | 9.39.4 → 10.13.0 | major, dev |
| `jsdom` | 29.1.1 → 30.1.3 | major, dev |
| `@testing-library/jest-dom` | 6.9.1 → 7.0.1 | major, dev |
| `@types/node` | 22.20.5 → 26.6.5 | major, dev |
| `node` (imagem) | 22-alpine → 26-alpine | **não aplicar** |
| `eclipse-temurin` (imagem) | 21-jre → 24-jre | **não aplicar** |

Os dois últimos apareceram porque o ecossistema `docker` passou a ser acompanhado
em §10, e revelaram um problema da própria configuração: a runtime das imagens tem
de casar com a declarada na CI (Java 21, Node 22, Python 3.12). Subir para Java 24
ou Node 26 por PR automático quebraria essa paridade sem decisão humana, então a
entrada `docker` passou a ignorar atualizações major — essa troca é feita junto com
a da CI, à mão.

Nenhum destes é correção de segurança com CVE aberta: o `pip-audit` do worker e o
`npm audit` do frontend estão limpos, e as três imagens finais seguem com zero
achados HIGH/CRITICAL.

---

## 14. RLS por tenant passa a valer de verdade — 28/10/2026

O gate de migração sobre dados existentes (§4 do C7, que faltava) revelou o achado
mais sério do dia: **o RLS era inteiramente decorativo**. O isolamento efetivo
vinha apenas dos filtros por tenant na camada de aplicação — que funcionam e
seguem valendo —, mas a segunda barreira prometida pelos docs não existia.

Três falhas sobrepostas, cada uma suficiente para anular as políticas:

1. **A aplicação conecta como superuser/owner.** Superuser ignora RLS sempre;
   owner ignora a menos que a tabela declare `FORCE ROW LEVEL SECURITY` — e
   nenhuma declarava.
2. **A política tinha fallback permissivo.** `COALESCE(NULLIF(current_setting(
   'app.current_tenant', true), ''), tenant_id) = tenant_id`: sem tenant no
   contexto, compara `tenant_id` com ele mesmo e libera todas as linhas.
3. **O tenant nunca estava no contexto das queries.** O interceptor
   `@TenantScopedRls` chamava um método `@Transactional` que abria transação
   própria, executava `set_config(..., true)` — local à transação — e commitava
   **antes** de o método real rodar. O setting evaporava ali.

As três foram corrigidas:

- **Migration 009**: `FORCE ROW LEVEL SECURITY` nas 13 tabelas com `tenant_id`;
  políticas recriadas sem o fallback e com `WITH CHECK` (gravar linha de outro
  tenant é recusado); role `archlens_app` (`NOSUPERUSER NOBYPASSRLS`) criado com
  GRANTs e `ALTER DEFAULT PRIVILEGES`, sem senha embutida — a senha vem do
  ambiente (`infra/postgres/init/02-app-role.sh` em instalação nova, `ALTER ROLE`
  manual em upgrade, conforme o runbook §9).
- **Runtime vs. migrations**: a aplicação conecta como `archlens_app`; o
  Liquibase usa `ARCHLENS_MIGRATION_DB_USERNAME/PASSWORD` (owner) para DDL.
- **Interceptor corrigido**: aplica o tenant **dentro** da transação em que as
  queries rodam — participa da existente ou abre uma para o método.

Prova com role não-superuser (SQL direto): `tenant-outro` vê 1 projeto,
`tenant-antigo` vê o outro, **sem tenant no contexto vê 0** (antes: tudo), e
`INSERT` com `tenant_id` alheio falha com *violates row-level security policy*.
Na stack completa: jornada inteira verde (login → upload → ingestão → análise com
5 riscos → PDF), cross-tenant 404 com e sem header forjado, cancelamento
idempotente e erasure zerando banco e storage.

### Gate de migração sobre dados existentes (executado)

Changesets 1–4 aplicados a um banco novo, dados de dois tenants inseridos no
schema antigo (inclusive projeto sem as colunas `status`/`file_count`, que ainda
não existiam), e então o upgrade completo (005→009, 17 changesets) por cima:
zero erros, totais preservados, colunas novas com default correto nas linhas
antigas, 15 tabelas e 13 políticas ao final — e o RLS novo valendo também para
os dados antigos.

### SQLAlchemy 2.1.6 e a dependência que faltava

O PR do Dependabot para SQLAlchemy 2.1.6 falhava com *"requires greenlet"*: o
projeto usa `sqlalchemy.ext.asyncio`, mas declarava só `sqlalchemy` — na série
2.0 o greenlet vinha de carona e na 2.1 deixou de vir. O requirements passou a
declarar `sqlalchemy[asyncio]==2.1.6`; suíte do worker 77/77 e `pip-audit`
limpo.

### Limite conhecido

O worker ainda conecta com o owner do schema (interno, sem porta publicada,
tenant validado pelo backend antes de chegar nele). Movê-lo para um role
restrito com contexto de tenant por sessão fica registrado como melhoria.
