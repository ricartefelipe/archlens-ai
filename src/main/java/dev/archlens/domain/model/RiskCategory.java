package dev.archlens.domain.model;

public enum RiskCategory {

    LACK_OF_OBSERVABILITY("Falta de Observabilidade"),
    CONTRACT_VIOLATION("Violação de Contrato"),
    DESTRUCTIVE_MIGRATION("Migração Destrutiva"),
    ROLLBACK_RISK("Risco de Rollback"),
    EXCESSIVE_COUPLING("Acoplamento Excessivo"),
    LACK_OF_IDEMPOTENCY("Falta de Idempotência"),
    MISSING_DLQ_RETRY("DLQ/Retry Ausente"),
    DOMAIN_ENTITY_LEAK("Vazamento de Entidade de Domínio"),
    OPENAPI_INCONSISTENCY("Inconsistência OpenAPI"),
    MISSING_CORRELATION_ID("Correlation ID Ausente"),
    LAYER_SEPARATION_ISSUE("Problema de Separação de Camadas");

    private final String label;

    RiskCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
