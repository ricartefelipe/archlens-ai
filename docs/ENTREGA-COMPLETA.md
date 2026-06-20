# Checklist de Entrega — ArchLens AI

**Data:** junho/2026

## Avaliação e documentação

| # | Item | Arquivo | Status |
|---|------|---------|--------|
| 1 | Avaliação ponta a ponta | `docs/AVALIACAO-PONTA-A-PONTA.md` | ✅ |
| 2 | README comercial | `docs/README-COMERCIAL.md` | ✅ |
| 3 | Template relatório cliente | `docs/TEMPLATE-RELATORIO-DIAGNOSTICO.md` | ✅ |
| 4 | Runbook consultor | `docs/RUNBOOK-CONSULTOR.md` | ✅ |
| 5 | Roadmap produto | `docs/ROADMAP-PRODUTO.md` | ✅ |
| 6 | Variáveis ambiente | `.env.example` | ✅ |

## Correções técnicas

| # | Item | Status |
|---|------|--------|
| 7 | Ingestão automática pós-upload | ✅ |
| 8 | Status INGESTING → READY | ✅ |
| 9 | Isolamento tenant (reads) | ✅ |
| 10 | Export relatório Markdown/JSON | ✅ |
| 11 | Bloqueio análise se projeto não pronto | ✅ |
| 12 | LLM fallback desligado em prod | ✅ |
| 13 | Worker default no docker-compose | ✅ |
| 14 | Auth middleware frontend | ✅ |
| 15 | Login Keycloak opcional | ✅ |
| 16 | Botões export na UI | ✅ |

## Pendente (roadmap)

| # | Item | Fase |
|---|------|------|
| 17 | PDF branded | 2 |
| 18 | OIDC PKCE completo | 2 |
| 19 | RLS Postgres | 2 |
| 20 | Testes E2E Playwright | 2 |
| 21 | Billing Stripe | 3 |
| 22 | Demo gravada | 2 |

---

**Conclusão:** entrega mínima para operar consultoria de diagnóstico arquitetural com ciclo E2E funcional e entregável exportável. Itens 17–22 são evoluções para escala comercial.
