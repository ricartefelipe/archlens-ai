# Roteiro de demo — ArchLens (3 minutos)

Use este roteiro para gravar a demonstração comercial do fluxo ponta a ponta.

## Preparação (antes de gravar)

```bash
docker compose up -d
# Aguardar postgres, rabbitmq, worker, backend e frontend healthy
open http://localhost:3000
```

Tenant de demo: `tenant-1` (modo dev) ou usuário Keycloak `architect@archlens.dev`.

Tenha um ZIP pequeno de repositório Java/Spring com:
- 1 controller grande
- 1 migration SQL com DELETE sem WHERE
- 1 Dockerfile com tag `latest`

## Cena 1 — Problema (0:00–0:25)

**Narração:** "Times que herdam sistemas não sabem onde estão os riscos arquiteturais. ArchLens ingere o repositório e entrega diagnóstico com evidências."

Mostrar tela de login → projetos vazios.

## Cena 2 — Ingestão (0:25–1:00)

1. Criar projeto `demo-checkout`
2. Upload do ZIP
3. Mostrar status passando para **READY** (ingestão concluída)
4. Listar arquivos classificados

## Cena 3 — Análise (1:00–1:50)

1. Disparar análise
2. Aguardar **COMPLETED**
3. Mostrar matriz de severidades (CRITICAL/HIGH/...)
4. Abrir 1 risco com evidência de arquivo

## Cena 4 — ADRs e chat (1:50–2:30)

1. Rolar até ADRs recomendados
2. Abrir chat: pergunta "Quais riscos existem na camada de API?"
3. Mostrar resposta contextual

## Cena 5 — Entrega (2:30–3:00)

1. Clicar **Exportar PDF** (ou Markdown)
2. Abrir arquivo — sumário, matriz, riscos
3. **Fechamento:** "Relatório pronto para curadoria e entrega ao cliente em consultoria de diagnóstico arquitetural."

## Checklist pós-gravação

- [ ] Blur de dados sensíveis no ZIP
- [ ] Logo do cliente no PDF (`ARCHLENS_REPORT_LOGO_URL`) se for case real
- [ ] Publicar no README comercial ou landing
