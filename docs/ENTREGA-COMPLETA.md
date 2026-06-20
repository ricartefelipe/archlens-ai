# Checklist de Entrega — ArchLens AI

**Data:** junho/2026

## Fase 1 — MVP consultoria ✅

| # | Item | Status |
|---|------|--------|
| 1 | Upload → ingest automático | ✅ |
| 2 | Isolamento tenant (reads) | ✅ |
| 3 | Export Markdown/JSON | ✅ |
| 4 | Fallback LLM off em prod | ✅ |
| 5 | Docs comercial + runbook + template | ✅ |
| 6 | Worker no compose | ✅ |

## Fase 2 — Confiança operacional ✅

| # | Item | Status |
|---|------|--------|
| 7 | Export PDF + logo (`ARCHLENS_REPORT_LOGO_URL`) | ✅ |
| 8 | OIDC PKCE (`NEXT_PUBLIC_OIDC_ENABLED`) | ✅ |
| 9 | RLS PostgreSQL (migration 005) | ✅ |
| 10 | E2E Playwright (`e2e/`) | ✅ |
| 11 | Docker backend + frontend no compose | ✅ |
| 12 | Roteiro demo (`docs/DEMO-SCRIPT.md`) | ✅ |

## Fase 3 — Pendente

| # | Item |
|---|------|
| 13 | Billing Stripe |
| 14 | Quotas por tenant |
| 15 | Landing integrada |

---

Documentação: [AVALIACAO-PONTA-A-PONTA.md](./AVALIACAO-PONTA-A-PONTA.md) · [ROADMAP-PRODUTO.md](./ROADMAP-PRODUTO.md)
