# Template — Relatório de Diagnóstico Arquitetural

Use este template como base do entregável ao cliente. O export automático do ArchLens (`GET /v1/projects/{id}/analyses/{id}/report?format=markdown`) já segue estrutura similar — **curadoria humana obrigatória** antes da entrega.

---

# Relatório de Diagnóstico Arquitetural

**Cliente:** [Nome / anonimizado]  
**Sistema:** [Nome do repositório ou produto]  
**Consultor:** Felipe Ricarte Magalhães  
**Data:** [DD/MM/AAAA]  
**Classificação:** Confidencial

---

## 1. Sumário executivo

[3–5 parágrafos em linguagem de negócio: contexto, principais achados, urgência, recomendação macro]

**Veredito em uma linha:** [Ex.: "Arquitetura operável com 3 riscos críticos de acoplamento e dívida em contratos API."]

---

## 2. Escopo e metodologia

| Item | Detalhe |
|------|---------|
| Repositórios analisados | [URLs / nomes] |
| Artefatos | Código, OpenAPI, migrations, Docker, pipelines CI |
| Ferramenta | ArchLens AI (análise estática + RAG) |
| Curadoria | Revisão sênior dos achados automáticos |
| Limitações | [Ex.: sem runtime, sem pentest, sem dados de produção] |

---

## 3. Matriz de riscos (priorizada)

| # | Severidade | Categoria | Título | Arquivo / evidência |
|---|------------|-----------|--------|---------------------|
| 1 | CRITICAL | ... | ... | `path/to/file` |
| 2 | HIGH | ... | ... | ... |

---

## 4. Detalhamento dos riscos

### 4.1 [Título do risco]

- **Severidade:** CRITICAL | HIGH | MEDIUM | LOW
- **Categoria:** [enum ArchLens]
- **Descrição:** ...
- **Evidência:** trecho ou referência
- **Impacto de negócio:** ...
- **Recomendação:** ...
- **Esforço estimado:** P | M | G

*(Repetir por risco)*

---

## 5. ADRs recomendados

### ADR-001 — [Título]

**Contexto:** ...  
**Decisão:** ...  
**Consequências:** ...

---

## 6. Plano de ação sugerido

| Fase | Prazo | Ações | Responsável sugerido |
|------|-------|-------|----------------------|
| Quick wins | 0–30 dias | ... | ... |
| Estrutural | 30–90 dias | ... | ... |
| Estratégico | 90+ dias | ... | ... |

---

## 7. Anexos

- Export JSON/Markdown bruto do ArchLens
- Logs de ingestão (se relevante)
- Glossário

---

_Gerado com apoio de ArchLens AI. Achados validados por consultor sênior._
