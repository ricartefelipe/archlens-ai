export interface Project {
  id: string;
  tenantId: string;
  name: string;
  description: string;
  status: 'CREATED' | 'UPLOADING' | 'UPLOADED' | 'INGESTING' | 'READY' | 'FAILED';
  fileCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectFile {
  id: string;
  projectId: string;
  filePath: string;
  fileType: string;
  fileTypeLabel?: string;
  sizeBytes: number;
  contentHash: string;
  createdAt: string;
}

export interface Analysis {
  id: string;
  projectId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  summary: string | null;
  risks: ArchitecturalRisk[];
  createdAt: string;
  updatedAt: string;
}

export interface ArchitecturalRisk {
  id: string;
  category: string;
  categoryLabel?: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  title: string;
  description: string;
  filePath: string;
  evidence: string;
  suggestion: string;
}

export interface Adr {
  id: string;
  analysisId: string;
  title: string;
  context: string;
  decision: string;
  consequences: string;
  status: 'PROPOSED' | 'ACCEPTED' | 'DEPRECATED' | 'SUPERSEDED';
  relatedFindings: string[];
  createdAt: string;
}

export interface Question {
  id: string;
  analysisId: string;
  question: string;
  answer: string;
  sources: string | null;
  createdAt: string;
}

export interface AccountUsage {
  tenantId: string;
  plan: string;
  planDisplayName: string;
  status: string;
  projectsUsed: number;
  projectsLimit: number;
  analysesUsed: number;
  analysesLimit: number;
  uploadMbUsed: number;
  uploadMbLimit: number;
  usagePeriodStart: string;
}

export interface AnalysisComparison {
  projectId: string;
  baseline: AnalysisSummary;
  current: AnalysisSummary;
  baselineSeverityCounts: Record<string, number>;
  currentSeverityCounts: Record<string, number>;
  added: ArchitecturalRisk[];
  removed: ArchitecturalRisk[];
  severityChanged: SeverityChangedRisk[];
  unchanged: ArchitecturalRisk[];
}

export interface AnalysisSummary {
  id: string;
  createdAt: string;
  riskCount: number;
}

export interface SeverityChangedRisk {
  baselineRisk: ArchitecturalRisk;
  currentRisk: ArchitecturalRisk;
}

export type OrgMemberRole = 'ORG_ADMIN' | 'ORG_MEMBER' | 'ORG_VIEWER';

export type OrgMemberStatus = 'ACTIVE' | 'INVITED' | 'REMOVED';

export interface OrgMember {
  id: string;
  tenantId: string;
  email: string;
  role: OrgMemberRole;
  status: OrgMemberStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrgInvite {
  id: string;
  tenantId: string;
  email: string;
  role: OrgMemberRole;
  expiresAt: string;
  acceptedAt: string | null;
  createdAt: string;
}

export interface ApiKey {
  id: string;
  tenantId: string;
  name: string;
  keyPrefix: string;
  scopes: string;
  createdAt: string;
  revokedAt: string | null;
  lastUsedAt: string | null;
}

export interface CreateApiKeyResponse extends ApiKey {
  plainKey: string;
}

export interface TenantWebhook {
  id: string;
  tenantId: string;
  url: string;
  events: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminTenant {
  tenantId: string;
  plan: string;
  planDisplayName: string;
  status: string;
  projectsUsed: number;
  projectsLimit: number;
  analysesUsed: number;
  analysesLimit: number;
  uploadMbUsed: number;
  uploadMbLimit: number;
  usagePeriodStart: string;
}
