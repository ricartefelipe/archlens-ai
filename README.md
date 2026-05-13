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

## Decisões de arquitetura (resumo)

1. **Portas para inferência e vetores**: `LlmGateway` e `EmbeddingGateway` com implementação local para desenvolvimento; troca de implementação via CDI/configuração.
2. **Multi-tenancy**: coluna `tenant_id`; em produção o tenant pode vir do token OIDC.
3. **Schema**: Liquibase em YAML; Hibernate em modo validação alinhado ao Liquibase.

## Licença

Projeto de portfólio. Todos os direitos reservados.
