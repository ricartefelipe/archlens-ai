/** Rótulos PT-BR para enums expostos pela API. */

export const STATUS_LABELS: Record<string, string> = {
  CREATED: 'Criado',
  UPLOADING: 'Enviando',
  UPLOADED: 'Enviado',
  INGESTING: 'Indexando',
  READY: 'Pronto',
  FAILED: 'Falhou',
  PENDING: 'Pendente',
  PROCESSING: 'Processando',
  COMPLETED: 'Concluído',
  PROPOSED: 'Proposto',
  ACCEPTED: 'Aceito',
  DEPRECATED: 'Obsoleto',
  SUPERSEDED: 'Substituído',
};

export const SEVERITY_LABELS: Record<string, string> = {
  CRITICAL: 'Crítico',
  HIGH: 'Alto',
  MEDIUM: 'Médio',
  LOW: 'Baixo',
};

export const RISK_CATEGORY_LABELS: Record<string, string> = {
  LACK_OF_OBSERVABILITY: 'Observabilidade',
  CONTRACT_VIOLATION: 'Contrato / API',
  DESTRUCTIVE_MIGRATION: 'Migration destrutiva',
  ROLLBACK_RISK: 'Risco de rollback',
  EXCESSIVE_COUPLING: 'Acoplamento',
  LACK_OF_IDEMPOTENCY: 'Idempotência',
  MISSING_DLQ_RETRY: 'DLQ / retry',
  DOMAIN_ENTITY_LEAK: 'Vazamento de domínio',
  OPENAPI_INCONSISTENCY: 'OpenAPI',
  MISSING_CORRELATION_ID: 'Correlação',
  LAYER_SEPARATION_ISSUE: 'Camadas',
  SECURITY_RISK: 'Segurança',
  MISSING_HEALTH_CHECK: 'Health check',
  MISSING_TEST_COVERAGE: 'Testes',
};

export function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}

export function severityLabel(severity: string): string {
  return SEVERITY_LABELS[severity] ?? severity;
}

export function riskCategoryLabel(category: string): string {
  return RISK_CATEGORY_LABELS[category] ?? category.replace(/_/g, ' ').toLowerCase();
}
