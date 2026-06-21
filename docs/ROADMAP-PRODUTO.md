# Roadmap de Produto — ArchLens AI

## Fase 1 — MVP vendável consultoria ✅

- [x] Upload → ingest automático → status READY
- [x] Isolamento tenant nos reads
- [x] Export Markdown/JSON
- [x] Desabilitar fallback LLM mock em prod
- [x] Documentação comercial + runbook + template
- [x] Worker no compose por padrão

## Fase 2 — Confiança operacional ✅

- [x] PDF com logo do cliente (`ARCHLENS_REPORT_LOGO_URL`)
- [x] OIDC PKCE no frontend (`NEXT_PUBLIC_OIDC_ENABLED=true`)
- [x] RLS PostgreSQL por tenant_id (migration 005 + interceptor)
- [x] Testes E2E Playwright (`e2e/`)
- [x] Dockerfile backend + frontend no compose
- [x] Roteiro de demo gravada (`docs/DEMO-SCRIPT.md`)

## Fase 3 — Monetização (consultoria) ✅

- [x] Quotas por tenant (projetos, análises/mês, upload MB)
- [x] Planos comerciais (PILOT → PORTFOLIO + INTERNO)
- [x] API `/v1/account/usage` + admin upgrade de plano
- [x] Painel de uso no frontend + link landing
- [x] ADR modelo comercial (`docs/ADR-001-modelo-comercial.md`)
- [x] UI admin de tenants (`/admin/tenants`)

## Fase 3b — Adiado (estratégia consultoria)

- [ ] Stripe: pay-per-diagnóstico
- [ ] Landing page integrada (archlens-landing)
- [ ] Dashboard de custo IA

## Fase 4 — Escala ✅

- [x] Portal org/RBAC (membros, convites, roles)
- [x] API pública (API keys + OpenAPI)
- [x] Webhooks (`analysis.completed` / `analysis.failed`)
- [x] White-label (env vars UI + PDF)
- [x] Analyzer Terraform (`.tf`, `.tfvars`)
- [x] Analyzer Kubernetes (manifests YAML)
- [x] Analyzer .NET (`.cs`)
- [x] Comparativo before/after entre análises
- [x] Export comparativo (Markdown/JSON/PDF)
- [x] Runbook produção (`docs/RUNBOOK-PRODUCAO.md`)

## Fase 5 — SaaS (opcional, se mudar estratégia)

- [ ] Onboarding self-service
- [ ] Billing recorrente
- [ ] SLA e suporte tiered
