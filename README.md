# ArchLens AI

Plataforma para diagnóstico arquitetural e apoio à modernização de aplicações.

O ArchLens combina análise estática de código e artefatos técnicos, consulta contextual sobre documentação e geração de relatórios com evidências rastreáveis. Código, contratos OpenAPI, migrations, Dockerfiles e pipelines entram na análise para dar contexto às conclusões e reduzir recomendações baseadas apenas em suposição.

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

Estrutura em camadas (**domínio → aplicação → infraestrutura → interfaces**), com portas (hexagonal) para persistência, inferência textual remota opcional, armazenamento de vetores de documentos e integrações externas.

```text
dev.archlens/
├── domain/              # Modelos e exceções
├── application/         # Casos de uso e portas (in/out)
├── infrastructure/      # JPA, mensageria, clientes HTTP
└── interfaces/rest/     # JAX-RS, DTOs, filtros
```

Os gateways de inferência e de vetores em modo local usam implementações em memória adequadas a desenvolvimento. Em produção, podem ser substituídos por adaptadores configuráveis (`archlens.llm.*` e provedores do worker em `worker-ai`) sem alterar o núcleo de domínio.

## Fluxo Git

- **`develop`**: integração contínua. Alterações chegam por **pull request** a partir de `feature/*`, `release/*` ou `hotfix/*`.
- **`main`**: estado estável, atualizado por **pull request** após as entregas.
- **Releases**: após merge em `main`, uma tag semântica `vMAJOR.MINOR.PATCH` dispara o workflow [`.github/workflows/release.yml`](.github/workflows/release.yml), que executa novamente os gates do CI e publica a GitHub Release.
- Ramos `feature/*`, `hotfix/*` e `release/*` são removidos do remoto após o merge.

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

## Execução

```bash
docker compose up -d
./mvnw quarkus:dev
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Health: `http://localhost:8080/q/health`

Header opcional: `X-Tenant-Id` (fallback `default`). As respostas incluem `X-Correlation-Id`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Interface: `http://localhost:3000`.

Variável opcional: `NEXT_PUBLIC_API_URL` (padrão `http://localhost:8080`).

## Exemplos de API

```bash
curl -X POST http://localhost:8080/v1/projects \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{"name": "exemplo", "description": ""}'

curl http://localhost:8080/v1/projects \
  -H "X-Tenant-Id: tenant-1"
```

## Inferência em produção

O serviço Quarkus escolhe o adapter de `LlmGateway` por meio de `archlens.llm.provider`.

| Valor | Comportamento |
|--------|---------------|
| `local` | Respostas determinísticas com `LocalLlmGateway`, sem chamadas HTTP |
| `openai` | Completions na URL e credenciais definidas por `ARCHLENS_LLM_OPENAI_*`; sem chave válida, ocorre fallback para `local` com aviso em log |
| `ollama` | Usa `/api/chat` no servidor definido por `ARCHLENS_LLM_OLLAMA_*` |

Variáveis principais:

- `ARCHLENS_LLM_PROVIDER` — `local` \| `openai` \| `ollama`
- `ARCHLENS_LLM_OPENAI_API_KEY`
- `ARCHLENS_LLM_OPENAI_BASE_URL`
- `ARCHLENS_LLM_OPENAI_MODEL`
- `ARCHLENS_LLM_OLLAMA_BASE_URL`
- `ARCHLENS_LLM_OLLAMA_MODEL`

No perfil `prod`, o `application.yml` recebe datasource, OIDC, CORS e URL do worker por variáveis de ambiente:

- `QUARKUS_DATASOURCE_*`
- `OIDC_AUTH_SERVER_URL`
- `CORS_ORIGINS`
- `WORKER_AI_BASE_URL`

O worker Python utiliza `EMBEDDING_PROVIDER` (`local`, `openai` ou `ollama`) em `worker-ai`. A dimensão do modelo deve permanecer alinhada à coluna `vector(N)` no PostgreSQL.

### Vetores no PostgreSQL

A coluna `document_chunks.embedding` é criada como `vector(N)` na migration `001`, com **N = 1536** por padrão.

| Onde | Variável |
|------|----------|
| Worker | `EMBEDDING_DIMENSION` ou `ARCHLENS_EMBEDDING_DIMENSION` |
| Backend Quarkus | `ARCHLENS_EMBEDDING_DIMENSION` → `archlens.embedding.dimension` |
| Liquibase / banco | `vector(1536)` em `001` |

Na inicialização, o worker executa por padrão uma verificação da dimensão do vetor. Se `len(vetor) != EMBEDDING_DIMENSION`, a aplicação falha cedo em vez de persistir vetores incompatíveis.

Para desativar essa verificação em desenvolvimento:

```bash
EMBEDDING_DIMENSION_VERIFY=false
```

Referência operacional:

| Modelo | Dimensão típica |
|--------|-----------------|
| `text-embedding-3-small` | 1536 |
| `text-embedding-3-large` | 3072 |
| `nomic-embed-text` | 768 |

Alterar a dimensão em uma base existente exige uma migration deliberada para `vector(N)` e um plano de reingestão dos documentos já vetorizados.

## Integração contínua

No **push** ou **pull request** para `main` e `develop`, [`.github/workflows/ci.yml`](.github/workflows/ci.yml) executa três jobs em paralelo:

- **Backend**: Java 21 (Eclipse Temurin), cache Maven e `./mvnw -B clean verify -DskipITs=true`
- **Frontend**: Node 22, `npm ci`, `npm run lint` e `npm run build`
- **worker-ai**: Python 3.12, instalação das dependências, `compileall` e validação de import do `app.main`

## Decisões de arquitetura

1. **Portas para inferência e vetores**  
   `LlmGateway` possui implementação local por padrão e adaptadores remotos configuráveis. O núcleo da aplicação não depende diretamente do provedor.

2. **Multi-tenancy**  
   O isolamento é feito por `tenant_id`. Em produção, o tenant pode ser derivado do token OIDC.

3. **Schema**  
   O banco é versionado com Liquibase. O Hibernate opera em modo de validação para evitar divergência entre entidades e migrations.

## Documentação

- [Avaliação ponta a ponta](docs/AVALIACAO-PONTA-A-PONTA.md)
- [README comercial](docs/README-COMERCIAL.md)
- [Runbook do consultor](docs/RUNBOOK-CONSULTOR.md)
- [Template de relatório](docs/TEMPLATE-RELATORIO-DIAGNOSTICO.md)
- [Roadmap](docs/ROADMAP-PRODUTO.md)
- [Checklist de entrega](docs/ENTREGA-COMPLETA.md)

## Licença

Projeto de portfólio. Todos os direitos reservados.
