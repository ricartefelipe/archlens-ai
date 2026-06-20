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

## Fase 3 — Quotas e planos comerciais ✅

| # | Item | Status |
|---|------|--------|
| 13 | Quotas por tenant (projetos, análises, upload) | ✅ |
| 14 | Planos PILOT/DIAGNOSTICO/PORTFOLIO/INTERNO | ✅ |
| 15 | API usage + admin upgrade | ✅ |
| 16 | Painel de uso no frontend | ✅ |
| 17 | ADR modelo comercial | ✅ |

## Fase 3b — Pendente

| # | Item |
|---|------|
| 18 | Billing Stripe |
| 19 | Landing integrada |
| 20 | Dashboard custo IA |

---

Documentação: [AVALIACAO-PONTA-A-PONTA.md](./AVALIACAO-PONTA-A-PONTA.md) · [ROADMAP-PRODUTO.md](./ROADMAP-PRODUTO.md)
