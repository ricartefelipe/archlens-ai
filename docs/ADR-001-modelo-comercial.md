# ADR-001 — Modelo comercial e quotas por tenant

**Status:** Aceito  
**Data:** junho/2026

## Contexto

ArchLens AI é uma plataforma de **consultoria empacotada** (diagnóstico arquitetural R$ 8k–60k), não um SaaS self-service. O consultor usa a ferramenta para entregar valor ao cliente final; a plataforma precisa:

1. Proteger margem (custo LLM, storage, processamento).
2. Refletir pacotes comerciais negociados offline.
3. Auto-provisionar novos tenants sem fricção operacional.

Pagamento online (Stripe) fica fora do escopo imediato — upgrade de plano é feito pelo consultor via API admin após contrato assinado.

## Decisão

### Planos comerciais (`CommercialPlan`)

| Plano | Projetos | Análises/mês | Upload MB/mês | Uso |
|-------|----------|--------------|---------------|-----|
| PILOT | 1 | 1 | 50 | Avaliação / primeiro contato |
| DIAGNOSTICO | 3 | 5 | 200 | Pacote padrão (R$ 8k–18k) |
| PORTFOLIO | 10 | 20 | 500 | Auditoria de portfólio |
| INTERNO | ∞ | ∞ | 500 | Uso do consultor |

### Provisionamento

- Primeiro acesso ao tenant → conta `PILOT` criada automaticamente (`QuotaService.ensureAccount`).
- Contadores mensais (`analyses_used_period`, `upload_bytes_period`) resetam no 1º dia do mês.
- Enforcement configurável: `archlens.commercial.enforce-quotas` (default `true`; `false` em `%test`).

### Enforcement

Quotas verificadas **antes** da operação:

- `ProjectService.create` → limite de projetos
- `UploadService.upload` → limite de bytes no período
- `AnalysisService.create` → limite de análises no período

Contadores incrementados **após** sucesso:

- Upload → `recordUpload`
- Análise concluída → `recordAnalysis` (via `AnalysisConsumer`)

Excesso → `QuotaExceededException` → HTTP **402 Payment Required**.

### APIs

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/v1/account/usage` | Tenant autenticado |
| PUT | `/v1/admin/tenants/{tenantId}/plan` | Role `admin` |

### Frontend

Painel de uso na sidebar (`UsagePanel`) com link de upgrade para landing comercial (`NEXT_PUBLIC_LANDING_URL`).

## Consequências

**Positivas**

- Margem protegida sem billing complexo na Fase 3.
- Upgrade comercial alinhado ao fluxo consultor → contrato → API admin.
- Tenant piloto funciona out-of-the-box.

**Negativas / trade-offs**

- Stripe adiado — cobrança continua manual.
- Planos INTERNO com upload limitado (500 MB) — suficiente para uso interno, não ilimitado em storage.
- RLS em `tenant_accounts` não aplicado (tabela consultada pelo tenant_id explícito; admin bypass via role).

## Alternativas consideradas

1. **Stripe Checkout na Fase 3** — rejeitado por priorizar entrega consultoria; billing online na Fase 3b.
2. **SaaS self-service com trial** — rejeitado; modelo de negócio é consultoria empacotada.
3. **Quotas só por análise** — rejeitado; upload e projetos também impactam custo.

## Referências

- [ROADMAP-PRODUTO.md](./ROADMAP-PRODUTO.md)
- [README-COMERCIAL.md](./README-COMERCIAL.md)
- Migration `006-create-tenant-accounts.yaml`
