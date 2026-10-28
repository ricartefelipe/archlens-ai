# Runbook de Produção — ArchLens AI

**Objetivo:** implantar a plataforma para entrega de consultoria empacotada com isolamento multi-tenant, quotas e integrações.

---

## 1. Pré-requisitos

| Componente | Versão mínima |
|------------|---------------|
| PostgreSQL + pgvector | 16 |
| RabbitMQ | 3.12+ |
| Keycloak | 25+ |
| JVM | 21 |
| Node.js | 22 |
| Python | 3.12 |

---

## 2. Variáveis de ambiente (produção)

### Backend

```bash
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://HOST:5432/archlens
QUARKUS_DATASOURCE_USERNAME=archlens
QUARKUS_DATASOURCE_PASSWORD=<secret>
OIDC_AUTH_SERVER_URL=https://auth.seudominio.com/realms/archlens
CORS_ORIGINS=https://app.seudominio.com
WORKER_AI_BASE_URL=http://worker-ai:8081
ARCHLENS_STORAGE_BASE_PATH=/data/archlens/projects
ARCHLENS_LLM_PROVIDER=openai
ARCHLENS_LLM_OPENAI_API_KEY=<secret>
ARCHLENS_ANALYSIS_LLM_FALLBACK=false
ARCHLENS_LLM_ALLOW_SIMULATED_DEMO=false
ARCHLENS_RLS_ENABLED=true
ARCHLENS_ENFORCE_QUOTAS=true
ARCHLENS_REPORT_BRAND_NAME=SuaMarca
ARCHLENS_REPORT_LOGO_URL=https://cdn.seudominio.com/logo.png
RABBITMQ_HOST=rabbitmq
RABBITMQ_USERNAME=archlens
RABBITMQ_PASSWORD=<secret>
```

### Frontend (build-time)

```bash
NEXT_PUBLIC_API_URL=https://api.seudominio.com
NEXT_PUBLIC_KEYCLOAK_URL=https://auth.seudominio.com
NEXT_PUBLIC_KEYCLOAK_REALM=archlens
NEXT_PUBLIC_KEYCLOAK_CLIENT=archlens-frontend
NEXT_PUBLIC_OIDC_ENABLED=true
NEXT_PUBLIC_APP_NAME=SuaMarca Diagnostics
NEXT_PUBLIC_APP_LOGO_URL=https://cdn.seudominio.com/logo.png
NEXT_PUBLIC_PRIMARY_COLOR=#2563eb
NEXT_PUBLIC_LANDING_URL=https://seudominio.com
NEXT_PUBLIC_SUPPORT_URL=mailto:consultoria@seudominio.com
BACKEND_ORIGIN=http://backend:8080
```

`BACKEND_ORIGIN` define o destino dos rewrites de `/v1`, `/public` e `/q` no
servidor Next. Com o nginx do `docker-compose.prod.yml` à frente, essas rotas não
chegam ao Next; a variável garante que o frontend funcione também se for exposto
sem o proxy.

**Origem das conclusões:** com `ARCHLENS_LLM_PROVIDER=local` não há inferência de
modelo externo, e cada conclusão produzida pelo fallback é marcada com essa origem
no relatório. Se `ARCHLENS_ANALYSIS_LLM_FALLBACK=true` for combinado com um gateway
simulado em produção, **a aplicação recusa iniciar** — a menos que
`ARCHLENS_LLM_ALLOW_SIMULATED_DEMO=true` seja definido explicitamente para uma demo.

### Worker-ai

```bash
DATABASE_URL=postgresql://archlens:<secret>@HOST:5432/archlens
STORAGE_BASE_PATH=/data/archlens/projects
EMBEDDING_PROVIDER=openai
OPENAI_API_KEY=<secret>
```

**Crítico:** `ARCHLENS_STORAGE_BASE_PATH` (backend) = `STORAGE_BASE_PATH` (worker) no mesmo volume.

---

## 3. Topologia recomendada

```
Internet
   │
   ├─ CDN / LB ──► Frontend (Next.js static ou container)
   │
   ├─ LB ──► Backend Quarkus :8080
   │            │
   │            ├── PostgreSQL (RDS / Cloud SQL)
   │            ├── RabbitMQ (Amazon MQ / CloudAMQP)
   │            └── Worker-ai :8081 (rede interna)
   │
   └─ Keycloak (realm archlens)
```

- Worker-ai **sem** exposição pública.
- TLS terminado no load balancer.
- Backups diários do Postgres (inclui embeddings pgvector).

---

## 4. Deploy (Docker Compose referência)

Para produção single-node ou staging local:

```bash
cp .env.example .env
# editar secrets e URLs
docker compose up -d --build
```

**AWS EC2 (piloto/prod — padrão Fluxe):** ver [DEPLOY-AWS-EC2.md](./DEPLOY-AWS-EC2.md).

Serviços: postgres, rabbitmq, keycloak, worker-ai, backend, frontend (+ nginx na EC2).

---

## 5. Pós-deploy

1. Importar realm Keycloak (`infra/keycloak/archlens-realm.json`) ou equivalente gerenciado.
2. Criar usuário platform `admin` com role `admin`.
3. Validar health: `GET /q/health`, worker `GET /health`.
4. Smoke: login → projeto → upload → análise → export PDF.
5. Configurar webhook de teste em **Configurações → Webhooks** (`analysis.completed`).
6. Upgrade de plano cliente via **Admin → Tenants** ou `PUT /v1/admin/tenants/{id}/plan`.

---

## 6. API pública (integrações)

Autenticação alternativa via header:

```http
X-Api-Key: alk_<prefix>_<secret>
```

Escopos: `read` (GET) e `write` (POST analyses, uploads). Documentação interativa: `/q/swagger-ui`.

---

## 7. Monitoramento

| Métrica | Onde |
|---------|------|
| HTTP / JVM | `/q/metrics` (Prometheus) |
| Filas RabbitMQ | management UI |
| Logs JSON | stdout → agregador (Loki/Datadog) |
| Quotas | `GET /v1/account/usage` por tenant |

---

## 8. Imagens base e scan de vulnerabilidades

As bases dos três Dockerfiles são fixadas por **digest** (manifest list, cobrindo
amd64 e arm64), para que o build seja reproduzível. Fixar o digest não corrige CVE:
quando houver correção na base, atualize o digest e **reconstrua** a imagem.

```bash
# Atualizar o digest de uma base
docker pull eclipse-temurin:21-jre
docker inspect eclipse-temurin:21-jre --format '{{index .RepoDigests 0}}'

# Reconstruir ignorando camadas em cache da base antiga
docker compose -f docker-compose.prod.yml build --pull backend frontend worker-ai
```

O Dependabot acompanha o ecossistema `docker` nos três diretórios e abre PR quando
a base avança — sem isso, um digest fixo deixaria de receber correções.

Scan das imagens finais (o mesmo critério usado no fechamento V4):

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy:latest image --scanners vuln --severity HIGH,CRITICAL <imagem>
```

Um scan em amd64 não comprova arm64: ao anunciar a plataforma, escaneie a imagem
construída para ela.

---

## 9. Rollback

1. Manter migration Liquibase forward-only; rollback = deploy versão anterior + backup DB se migration irreversível.
2. Worker e backend devem ser versionados juntos (contrato analyze/ingest).

---

## 10. Checklist go-live consultoria

- [ ] OIDC PKCE ativo no frontend
- [ ] RLS habilitado (`ARCHLENS_RLS_ENABLED=true`)
- [ ] Fallback LLM desligado
- [ ] Logo/marca no PDF configurados
- [ ] Quotas enforcement ativo
- [ ] Primeiro tenant PILOT provisionado (automático no uso)
- [ ] Runbook consultor (`docs/RUNBOOK-CONSULTOR.md`) entregue à equipe
