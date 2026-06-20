# Roadmap de Produto — ArchLens AI

## Fase 1 — MVP vendável consultoria ✅ (esta entrega)

- [x] Upload → ingest automático → status READY
- [x] Isolamento tenant nos reads
- [x] Export Markdown/JSON
- [x] Desabilitar fallback LLM mock em prod
- [x] Documentação comercial + runbook + template
- [x] Worker no compose por padrão

## Fase 2 — Confiança operacional (30 dias)

- [ ] PDF com logo do cliente
- [ ] OIDC PKCE no frontend (remover tenant manual em prod)
- [ ] RLS PostgreSQL por tenant_id
- [ ] Testes E2E Playwright
- [ ] Dockerfile backend + frontend no compose
- [ ] Demo gravada (3 min)

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
