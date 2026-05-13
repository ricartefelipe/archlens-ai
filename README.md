# ArchLens AI

Plataforma multi-tenant para análise arquitetural: combina recuperação de contexto em documentos (RAG), análise estática de artefatos (código, OpenAPI, migrations, Docker, pipelines) e relatórios com evidências rastreáveis.

**Código:** [github.com/ricartefelipe/archlens-ai](https://github.com/ricartefelipe/archlens-ai) — branches `main` e `develop` mantidas em sincronia (fluxo de integração típico `develop` → `main`).

## Stack

| Camada | Tecnologia |
|--------|------------|
| Backend | Java 21, Quarkus 3.35, REST |
| Dados | PostgreSQL 16, pgvector, Liquibase |
| Cache | Redis 7 |
| Autenticação | Keycloak (OIDC), JWT no backend |
| Observabilidade | OpenTelemetry, Micrometer/Prometheus, logs JSON |
| Interface | Next.js, React, TypeScript |
| Worker de ingestão | Python 3.12, FastAPI |

## Arquitetura

Estrutura em camadas (**domínio → aplicação → infraestrutura → interfaces**), com portas (hexagonal) para persistência, LLM, embeddings e integrações externas.

```
dev.archlens/
├── domain/              # Modelos e exceções
├── application/       # Casos de uso e portas (in/out)
├── infrastructure/     # JPA, mensageria, clientes HTTP
└── interfaces/rest/    # JAX-RS, DTOs, filtros
```

Os gateways de LLM e embedding em modo local usam implementações em memória adequadas a desenvolvimento; em produção substituem-se por adapters configurados (OpenAI, Ollama, etc.) sem alterar o núcleo de domínio.

## Pré-requisitos

- Java 21+, Maven 3.9+
- Docker e Docker Compose (infra local)

## Execução

```bash
docker compose up -d
./mvnw quarkus:dev
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Health: `http://localhost:8080/q/health`

Header opcional: `X-Tenant-Id` (fallback `default`). Respostas incluem `X-Correlation-Id`.

### Frontend

```bash
cd frontend && npm install && npm run dev
```

Interface: `http://localhost:3000`. Variável opcional: `NEXT_PUBLIC_API_URL` (padrão `http://localhost:8080`).

## Exemplos de API

```bash
curl -X POST http://localhost:8080/v1/projects \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{"name": "exemplo", "description": ""}'

curl http://localhost:8080/v1/projects -H "X-Tenant-Id: tenant-1"
```

## Fase 9 — Inferência em produção (configuração)

O serviço Quarkus escolhe o adapter de `LlmGateway` via `archlens.llm.provider`:

| Valor | Comportamento |
|--------|----------------|
| `local` (padrão dev) | Respostas determinísticas (`LocalLlmGateway`), sem chamadas HTTP. |
| `openai` | Chat Completions na API configurada (`ARCHLENS_LLM_OPENAI_*`). Sem API key válida, regressão automática para `local` com aviso em log. |
| `ollama` | `/api/chat` no servidor Ollama (`ARCHLENS_LLM_OLLAMA_*`). |

Variáveis úteis:

- `ARCHLENS_LLM_PROVIDER` — `local` \| `openai` \| `ollama`
- `ARCHLENS_LLM_OPENAI_API_KEY` — obrigatória se `provider=openai`
- `ARCHLENS_LLM_OPENAI_BASE_URL` — por defeito `https://api.openai.com` (compatível com proxies OpenAI-compatible)
- `ARCHLENS_LLM_OPENAI_MODEL` — por defeito `gpt-4o-mini`
- `ARCHLENS_LLM_OLLAMA_BASE_URL`, `ARCHLENS_LLM_OLLAMA_MODEL`

Perfil **`prod`**: `application.yml` lê datasource, OIDC, CORS e URL do worker-ai a partir de variáveis (`QUARKUS_DATASOURCE_*`, `OIDC_AUTH_SERVER_URL`, `CORS_ORIGINS`, `WORKER_AI_BASE_URL`).

O **worker Python** continua a usar `EMBEDDING_PROVIDER` (`local`, `openai`, `ollama`) em `worker-ai` — alinhar dimensão do modelo com pgvector se mudares de `text-embedding-3-small` (1536).

### Embeddings: dimensão = contrato com o PostgreSQL

A coluna `document_chunks.embedding` é `vector(N)` na migration `001` com **N = 1536** por defeito.  
O **worker** e o **backend** (para `LocalEmbeddingGateway`) usam a mesma referência:

| Onde | Variável |
|------|-----------|
| Worker | `EMBEDDING_DIMENSION` ou `ARCHLENS_EMBEDDING_DIMENSION` |
| Backend Quarkus | `ARCHLENS_EMBEDDING_DIMENSION` → `archlens.embedding.dimension` |
| Liquibase / BD | `vector(1536)` em `001` — alterar só com migration/migração deliberada |

Ao arranque, o worker faz (por defeito) um **probe**: gera um embedding e falha se `len(vetor) ≠ EMBEDDING_DIMENSION`. Para desligar em dev: `EMBEDDING_DIMENSION_VERIFY=false`.

**Modelos frequentes (referência):**

| Modelo / familia | Dimensão típica |
|------------------|------------------|
| OpenAI `text-embedding-3-small` | 1536 |
| OpenAI `text-embedding-3-large` | 3072 |
| Ollama `nomic-embed-text` | 768 |

**Mudar de dimensão em base existente:** requer `ALTER TABLE ... TYPE vector(N)` (com plano de **re-ingestão** ou embeddings incompatíveis) — não faz parte das migrations automáticas; faz backup antes.

## Fase 10 — CI (GitHub Actions)

No **push** ou **pull request** para `main` e `develop`, [`.github/workflows/ci.yml`](.github/workflows/ci.yml) executa três jobs em paralelo:

- **Backend**: Java 21 (Eclipse Temurin), cache Maven, `./mvnw -B clean verify -DskipITs=true`
- **Frontend**: Node 22, `npm ci`, `npm run lint`, `npm run build` (com `NEXT_TELEMETRY_DISABLED=1`)
- **worker-ai**: Python 3.12, `pip install -r requirements.txt`, `compileall` no pacote `app`, import de `app.main`

## Decisões de arquitetura (resumo)

1. **Portas para inferência e vetores**: `LlmGateway` com implementação local por defeito; **OpenAI/Ollama** via configuração (`archlens.llm.*`). `EmbeddingGateway` no JVM permanece local; embeddings reais no fluxo RAG são tratados pelo worker.
2. **Multi-tenancy**: coluna `tenant_id`; em produção o tenant pode vir do token OIDC.
3. **Schema**: Liquibase em YAML; Hibernate em modo validação alinhado ao Liquibase.

## Licença

Projeto de portfólio. Todos os direitos reservados.
