# ArchLens AI

Plataforma multi-tenant de inteligência arquitetural que analisa código-fonte, documentação técnica, contratos OpenAPI, migrations, arquivos Docker/Kubernetes e pipelines CI/CD usando RAG + análise estática para gerar diagnósticos arquiteturais com evidências rastreáveis.

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21, Quarkus 3.35, REST API |
| Banco de dados | PostgreSQL 16, pgvector, Liquibase YAML |
| Cache | Redis 7 |
| Auth | Keycloak 25 (futuro) |
| Observabilidade | OpenTelemetry, Micrometer/Prometheus, logs JSON |
| Frontend | Next.js, React, TypeScript (Fase 2) |
| Worker IA | Python 3.12, FastAPI (Fase 2) |

## Arquitetura

O projeto segue **Clean Architecture / Hexagonal Architecture / DDD leve**:

```
dev.archlens/
├── domain/              # Modelos e exceções de domínio
│   ├── model/
│   └── exception/
├── application/         # Portas e serviços
│   ├── port/
│   │   ├── in/          # Use cases (interfaces)
│   │   └── out/         # Repositories e gateways (interfaces)
│   └── service/         # Implementação dos use cases
├── infrastructure/      # Adaptadores concretos
│   ├── persistence/
│   │   ├── entity/      # Entidades JPA
│   │   ├── panache/     # Repositórios Panache
│   │   ├── adapter/     # Implementação das portas de repositório
│   │   └── mapper/      # Mapeadores entidade <-> domínio
│   └── gateway/         # Gateways fake (LLM, Embedding)
└── interfaces/          # Camada de apresentação
    └── rest/
        ├── dto/         # Request/Response DTOs
        ├── mapper/      # Mapeadores DTO <-> domínio
        ├── filter/      # Filtros HTTP (Correlation-Id, Tenant)
        ├── exception/   # Exception mappers globais
        └── context/     # Beans request-scoped (TenantProvider)
```

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

## Como rodar

### 1. Subir a infraestrutura

```bash
docker compose up -d
```

Aguarde os serviços ficarem saudáveis:

```bash
docker compose ps
```

### 2. Rodar a aplicação

```bash
mvn quarkus:dev
```

A aplicação sobe em `http://localhost:8080`.

### 3. Acessar documentação da API

- Swagger UI: http://localhost:8080/q/swagger-ui
- OpenAPI spec: http://localhost:8080/q/openapi
- Health: http://localhost:8080/q/health
- Metrics: http://localhost:8080/q/metrics

## Endpoints da API (Fase 1)

Todos os endpoints aceitam o header `X-Tenant-Id` (opcional, fallback: `default`).
Todas as respostas incluem o header `X-Correlation-Id`.

### Projetos

```bash
# Criar projeto
curl -X POST http://localhost:8080/v1/projects \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{"name": "meu-projeto", "description": "Descrição do projeto"}'

# Listar projetos
curl http://localhost:8080/v1/projects \
  -H "X-Tenant-Id: tenant-1"

# Buscar projeto
curl http://localhost:8080/v1/projects/{projectId} \
  -H "X-Tenant-Id: tenant-1"
```

### Análises

```bash
# Criar análise (usa gateway fake no MVP)
curl -X POST http://localhost:8080/v1/projects/{projectId}/analyses \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1"

# Buscar análise com riscos
curl http://localhost:8080/v1/projects/{projectId}/analyses/{analysisId} \
  -H "X-Tenant-Id: tenant-1"
```

### Perguntas RAG

```bash
# Fazer pergunta sobre a análise (usa gateway fake no MVP)
curl -X POST http://localhost:8080/v1/projects/{projectId}/analyses/{analysisId}/questions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{"question": "Quais são os principais riscos arquiteturais?"}'
```

## Decisões Arquiteturais (ADRs)

### ADR-001: Gateway Fake para LLM e Embedding no MVP

**Status**: Aceito

**Contexto**: Na Fase 1, o objetivo é validar a arquitetura e o fluxo de dados sem depender de serviços externos de IA.

**Decisão**: Criar interfaces `LlmGateway` e `EmbeddingGateway` com implementações fake (`FakeLlmGateway`, `FakeEmbeddingGateway`) que retornam dados simulados. A troca por OpenAI, Ollama ou outro provider será feita apenas substituindo a implementação do adapter, sem alterar domain ou application.

**Consequência**: O fluxo completo funciona localmente sem custo de API. A arquitetura está preparada para integração real via porta/adapter.

### ADR-002: Multi-tenancy via coluna tenant_id

**Status**: Aceito

**Contexto**: O sistema precisa suportar múltiplos tenants desde o início.

**Decisão**: Todas as tabelas principais possuem coluna `tenant_id`. O tenant é extraído do header `X-Tenant-Id` e injetado via `TenantProvider` nos serviços.

**Consequência**: Isolamento de dados por tenant. Na Fase 2, o tenant virá do token JWT do Keycloak.

### ADR-003: Liquibase YAML para migrations

**Status**: Aceito

**Contexto**: Controle versionado de schema é obrigatório. Não usar ddl-auto.

**Decisão**: Usar Liquibase com changelogs em YAML. Hibernate ORM configurado com `generation: none`.

**Consequência**: Schema controlado, auditável e reproduzível em qualquer ambiente.

## Roadmap

- [x] **Fase 1**: Backend Quarkus, PostgreSQL/pgvector, Liquibase, Docker Compose, endpoints REST, gateways fake
- [ ] **Fase 2**: Worker Python (FastAPI) para ingestão, chunking e embeddings reais
- [ ] **Fase 3**: Integração com LLM real (OpenAI/Ollama), RAG funcional
- [ ] **Fase 4**: Frontend Next.js
- [ ] **Fase 5**: RabbitMQ para processamento assíncrono
- [ ] **Fase 6**: Keycloak para autenticação/autorização
- [ ] **Fase 7**: Upload de .zip e análise de repositórios Git

## Licença

Projeto de portfólio. Todos os direitos reservados.
