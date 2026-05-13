from collections import defaultdict
from uuid import UUID

import structlog

from app.analysis.rules import AdrSuggestion, RiskCategory, RiskFinding, RiskSeverity

log = structlog.get_logger()

_ADR_TEMPLATES: dict[RiskCategory, dict[str, str]] = {
    RiskCategory.LACK_OF_OBSERVABILITY: {
        "title": "Implementar estratégia de observabilidade",
        "context": "A aplicação carece de métricas customizadas e instrumentação adequada, dificultando o monitoramento de indicadores de negócio e a detecção proativa de problemas.",
        "decision": "Adotar OpenTelemetry como padrão de instrumentação, expondo métricas via Prometheus e traces via Jaeger/Tempo. Instrumentar endpoints críticos com @Timed e counters de negócio.",
        "consequences": "Maior visibilidade operacional. Overhead de performance mínimo (~1-3%). Necessidade de infraestrutura de monitoramento (Grafana stack).",
    },
    RiskCategory.EXCESSIVE_COUPLING: {
        "title": "Reduzir acoplamento entre camadas",
        "context": "Controllers contêm lógica de negócio, violando a separação de responsabilidades e dificultando testes unitários e manutenção.",
        "decision": "Adotar padrão de Use Cases (Application Services) como intermediários entre controllers e domínio. Controllers devem apenas receber requests, delegar para use cases e retornar responses.",
        "consequences": "Melhor testabilidade (use cases podem ser testados sem HTTP). Maior número de classes. Necessidade de disciplina para manter a separação.",
    },
    RiskCategory.LAYER_SEPARATION_ISSUE: {
        "title": "Separar entidades de persistência da API REST",
        "context": "Entidades JPA estão sendo expostas diretamente nos endpoints REST, criando acoplamento entre esquema de banco e contrato de API.",
        "decision": "Criar DTOs (Records) específicos para cada endpoint. Usar mappers explícitos entre entidades de domínio e DTOs de apresentação.",
        "consequences": "Liberdade para evoluir schema do banco sem quebrar a API. Mais classes de mapeamento. Possibilidade de retornar apenas campos necessários.",
    },
    RiskCategory.DESTRUCTIVE_MIGRATION: {
        "title": "Adotar política de migrações seguras",
        "context": "Migrações de banco contêm operações destrutivas (DROP, DELETE) sem salvaguardas, criando risco de perda de dados em produção.",
        "decision": "Toda migração destrutiva deve usar IF EXISTS, incluir rollback plan documentado, e passar por review obrigatório. Evitar DROP COLUMN direto, preferir deprecação gradual.",
        "consequences": "Migrações mais seguras e reversíveis. Processo de review mais rigoroso. Possível acumulação de colunas deprecated temporariamente.",
    },
    RiskCategory.MISSING_HEALTH_CHECK: {
        "title": "Implementar health checks em todos os serviços",
        "context": "Containers e serviços não possuem health checks configurados, impedindo que orquestradores detectem falhas e façam recovery automático.",
        "decision": "Adicionar endpoints /health (liveness) e /ready (readiness) em cada serviço. Configurar HEALTHCHECK no Dockerfile e healthcheck no docker-compose.",
        "consequences": "Melhor resiliência em produção. Detecção automática de falhas. Necessidade de definir critérios claros de saúde.",
    },
    RiskCategory.SECURITY_RISK: {
        "title": "Implementar baseline de segurança",
        "context": "Foram identificados riscos de segurança: containers rodando como root, falta de security schemes na API, ausência de scans no pipeline.",
        "decision": "Adotar checklist de segurança: containers não-root, imagens com versão fixa, SAST no pipeline, security schemes na OpenAPI, dependências auditadas.",
        "consequences": "Redução significativa de superfície de ataque. Overhead no pipeline de CI. Necessidade de treinamento da equipe.",
    },
    RiskCategory.CONTRACT_VIOLATION: {
        "title": "Padronizar contratos de API",
        "context": "Especificações OpenAPI estão incompletas, sem respostas de erro documentadas e sem esquemas de segurança definidos.",
        "decision": "Toda API deve ter OpenAPI spec completa com: respostas 2xx, 4xx e 5xx documentadas, security schemes definidos, exemplos de request/response.",
        "consequences": "Contratos mais claros para consumidores. Possibilidade de validação automática. Necessidade de manter specs atualizadas.",
    },
    RiskCategory.MISSING_TEST_COVERAGE: {
        "title": "Estabelecer cobertura mínima de testes",
        "context": "Pipeline de CI/CD não possui etapa de testes automatizados, permitindo deploy de código sem verificação.",
        "decision": "Configurar execução obrigatória de testes no pipeline. Definir cobertura mínima de 70% para código de negócio. Incluir testes de integração para endpoints críticos.",
        "consequences": "Maior confiança em deploys. Tempo de pipeline mais longo. Necessidade de cultura de testes na equipe.",
    },
}


class AdrGenerator:
    def generate_adrs(self, findings: list[RiskFinding]) -> list[AdrSuggestion]:
        grouped: dict[RiskCategory, list[RiskFinding]] = defaultdict(list)
        for finding in findings:
            grouped[finding.category].append(finding)

        adrs: list[AdrSuggestion] = []
        for category, category_findings in grouped.items():
            max_severity = max(
                (f.severity for f in category_findings),
                key=lambda s: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].index(s.value),
            )
            if max_severity in (RiskSeverity.CRITICAL, RiskSeverity.HIGH, RiskSeverity.MEDIUM):
                template = _ADR_TEMPLATES.get(category)
                if template:
                    adr = AdrSuggestion(
                        title=template["title"],
                        context=template["context"],
                        decision=template["decision"],
                        consequences=template["consequences"],
                        related_findings=[f.id for f in category_findings],
                    )
                    adrs.append(adr)

        log.info("adrs_generated", count=len(adrs), categories=list(grouped.keys()))
        return adrs
