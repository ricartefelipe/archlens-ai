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

## Fase 3 — Monetização (60 dias)

- [ ] Stripe: pay-per-diagnóstico
- [ ] Quotas por tenant (uploads, análises, tokens)
- [ ] Metering e dashboard de custo IA
- [ ] Landing page integrada (archlens-landing)

## Fase 4 — Escala (90+ dias)

- [ ] Portal de organizações (convites, RBAC)
- [ ] Analyzers: Terraform, K8s, .NET
- [ ] Comparativo before/after entre análises
- [ ] API pública para integrações
- [ ] White-label

## Fase 5 — SaaS (opcional, se mudar estratégia)

- [ ] Onboarding self-service
- [ ] Billing recorrente
- [ ] SLA e suporte tiered
