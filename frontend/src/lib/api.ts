import type {
  Project,
  ProjectFile,
  Analysis,
  Adr,
  Question,
  AccountUsage,
  AnalysisComparison,
  OrgMember,
  OrgInvite,
  OrgInviteCreated,
  OrgMemberRole,
  ApiKey,
  CreateApiKeyResponse,
  TenantWebhook,
  AdminTenant,
} from './types';
import { getApiBase, newCorrelationId } from './api-base';
import { getAccessToken, redirectToLogin, refreshSession } from './auth';

function correlationId(): string {
  return newCorrelationId();
}

async function apiFetch<T>(path: string, tenantId: string, options: RequestInit = {}): Promise<T> {
  const res = await apiFetchResponse(path, tenantId, options);

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`API ${res.status}: ${text || res.statusText}`);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}

async function apiFetchResponse(
  path: string,
  tenantId: string,
  options: RequestInit = {},
  allowRefresh = true,
): Promise<Response> {
  const headers: Record<string, string> = {
    'X-Tenant-Id': tenantId,
    'X-Correlation-Id': correlationId(),
    ...(options.headers as Record<string, string> || {}),
  };

  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${getApiBase()}${path}`, { ...options, headers });

  if (res.status === 401 && allowRefresh && typeof window !== 'undefined') {
    const refreshed = await refreshSession();
    if (refreshed) {
      return apiFetchResponse(path, tenantId, options, false);
    }
    redirectToLogin();
  }

  return res;
}

export async function listProjects(tenantId: string): Promise<Project[]> {
  return apiFetch('/v1/projects', tenantId);
}

export async function getAccountUsage(tenantId: string): Promise<AccountUsage> {
  return apiFetch('/v1/account/usage', tenantId);
}

export async function compareAnalyses(
  tenantId: string,
  projectId: string,
  baselineAnalysisId: string,
  currentAnalysisId: string
): Promise<AnalysisComparison> {
  const params = new URLSearchParams({
    baseline: baselineAnalysisId,
    current: currentAnalysisId,
  });
  return apiFetch(`/v1/projects/${projectId}/analyses/compare?${params}`, tenantId);
}

export async function createProject(tenantId: string, name: string, description: string): Promise<Project> {
  return apiFetch('/v1/projects', tenantId, {
    method: 'POST',
    body: JSON.stringify({ name, description }),
  });
}

export async function getProject(tenantId: string, projectId: string): Promise<Project> {
  return apiFetch(`/v1/projects/${projectId}`, tenantId);
}

export async function uploadProjectZip(tenantId: string, projectId: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append('file', file);
  await apiFetch(`/v1/projects/${projectId}/upload`, tenantId, {
    method: 'POST',
    body: formData,
  });
}

export async function listProjectFiles(tenantId: string, projectId: string): Promise<ProjectFile[]> {
  return apiFetch(`/v1/projects/${projectId}/files`, tenantId);
}

export async function createAnalysis(tenantId: string, projectId: string): Promise<Analysis> {
  return apiFetch(`/v1/projects/${projectId}/analyses`, tenantId, { method: 'POST' });
}

export async function getAnalysis(tenantId: string, projectId: string, analysisId: string): Promise<Analysis> {
  return apiFetch(`/v1/projects/${projectId}/analyses/${analysisId}`, tenantId);
}

export async function listAnalyses(tenantId: string, projectId: string): Promise<Analysis[]> {
  return apiFetch(`/v1/projects/${projectId}/analyses`, tenantId);
}

export async function listAdrs(tenantId: string, projectId: string, analysisId: string): Promise<Adr[]> {
  return apiFetch(`/v1/projects/${projectId}/analyses/${analysisId}/adrs`, tenantId);
}

export async function askQuestion(
  tenantId: string,
  projectId: string,
  analysisId: string,
  question: string
): Promise<Question> {
  return apiFetch(
    `/v1/projects/${projectId}/analyses/${analysisId}/questions`,
    tenantId,
    {
      method: 'POST',
      body: JSON.stringify({ question }),
    }
  );
}

export async function listQuestions(
  tenantId: string,
  projectId: string,
  analysisId: string
): Promise<Question[]> {
  return apiFetch(
    `/v1/projects/${projectId}/analyses/${analysisId}/questions`,
    tenantId
  );
}

export function reportExportUrl(
  projectId: string,
  analysisId: string,
  format: 'markdown' | 'json' | 'pdf' = 'markdown'
): string {
  return `${getApiBase()}/v1/projects/${projectId}/analyses/${analysisId}/report?format=${format}`;
}

export async function downloadReport(
  tenantId: string,
  projectId: string,
  analysisId: string,
  format: 'markdown' | 'json' | 'pdf'
): Promise<void> {
  const headers: Record<string, string> = {
    'X-Tenant-Id': tenantId,
    'X-Correlation-Id': correlationId(),
  };
  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(reportExportUrl(projectId, analysisId, format), { headers });
  if (!res.ok) {
    throw new Error(`Export failed: ${res.status}`);
  }

  const blob = await res.blob();
  const disposition = res.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="([^"]+)"/);
  const fileName = match?.[1] ?? `archlens-report.${format === 'json' ? 'json' : format === 'pdf' ? 'pdf' : 'md'}`;

  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function comparisonReportUrl(
  projectId: string,
  baselineAnalysisId: string,
  currentAnalysisId: string,
  format: 'markdown' | 'json' | 'pdf' = 'markdown'
): string {
  const params = new URLSearchParams({
    baseline: baselineAnalysisId,
    current: currentAnalysisId,
    format,
  });
  return `${getApiBase()}/v1/projects/${projectId}/analyses/compare/report?${params}`;
}

export async function downloadComparisonReport(
  tenantId: string,
  projectId: string,
  baselineAnalysisId: string,
  currentAnalysisId: string,
  format: 'markdown' | 'json' | 'pdf'
): Promise<void> {
  const headers: Record<string, string> = {
    'X-Tenant-Id': tenantId,
    'X-Correlation-Id': correlationId(),
  };
  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(
    comparisonReportUrl(projectId, baselineAnalysisId, currentAnalysisId, format),
    { headers }
  );
  if (!res.ok) {
    throw new Error(`Export failed: ${res.status}`);
  }

  const blob = await res.blob();
  const disposition = res.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="([^"]+)"/);
  const fileName = match?.[1]
    ?? `archlens-comparison.${format === 'json' ? 'json' : format === 'pdf' ? 'pdf' : 'md'}`;

  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function listOrgMembers(tenantId: string): Promise<OrgMember[]> {
  return apiFetch('/v1/org/members', tenantId);
}

export async function removeOrgMember(tenantId: string, memberId: string): Promise<void> {
  await apiFetch(`/v1/org/members/${memberId}`, tenantId, { method: 'DELETE' });
}

export async function listOrgInvites(tenantId: string): Promise<OrgInvite[]> {
  return apiFetch('/v1/org/invites', tenantId);
}

export async function createOrgInvite(
  tenantId: string,
  email: string,
  role: OrgMemberRole
): Promise<OrgInviteCreated> {
  return apiFetch('/v1/org/invites', tenantId, {
    method: 'POST',
    body: JSON.stringify({ email, role }),
  });
}

export async function acceptOrgInvite(
  tenantId: string,
  token: string,
  email: string
): Promise<OrgMember> {
  return apiFetch('/v1/org/invites/accept', tenantId, {
    method: 'POST',
    body: JSON.stringify({ token, email }),
  });
}

export async function listApiKeys(tenantId: string): Promise<ApiKey[]> {
  return apiFetch('/v1/account/api-keys', tenantId);
}

export async function createApiKey(
  tenantId: string,
  name: string,
  scopes = 'read,write'
): Promise<CreateApiKeyResponse> {
  return apiFetch('/v1/account/api-keys', tenantId, {
    method: 'POST',
    body: JSON.stringify({ name, scopes }),
  });
}

export async function revokeApiKey(tenantId: string, keyId: string): Promise<void> {
  await apiFetch(`/v1/account/api-keys/${keyId}`, tenantId, { method: 'DELETE' });
}

export async function listWebhooks(tenantId: string): Promise<TenantWebhook[]> {
  return apiFetch('/v1/account/webhooks', tenantId);
}

export async function createWebhook(
  tenantId: string,
  url: string,
  events: string[] = ['analysis.completed']
): Promise<TenantWebhook> {
  return apiFetch('/v1/account/webhooks', tenantId, {
    method: 'POST',
    body: JSON.stringify({ url, events }),
  });
}

export async function deleteWebhook(tenantId: string, webhookId: string): Promise<void> {
  await apiFetch(`/v1/account/webhooks/${webhookId}`, tenantId, { method: 'DELETE' });
}

export async function listAdminTenants(tenantId: string): Promise<AdminTenant[]> {
  return apiFetch('/v1/admin/tenants', tenantId);
}

export async function getAdminTenant(tenantId: string, targetTenantId: string): Promise<AdminTenant> {
  return apiFetch(`/v1/admin/tenants/${targetTenantId}`, tenantId);
}

export async function updateAdminTenantStatus(
  tenantId: string,
  targetTenantId: string,
  status: 'ACTIVE' | 'SUSPENDED'
): Promise<AdminTenant> {
  return apiFetch(`/v1/admin/tenants/${targetTenantId}/status`, tenantId, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function upgradeAdminTenantPlan(
  tenantId: string,
  targetTenantId: string,
  plan: string,
  notes?: string
): Promise<AccountUsage> {
  return apiFetch(`/v1/admin/tenants/${targetTenantId}/plan`, tenantId, {
    method: 'PUT',
    body: JSON.stringify({ plan, notes: notes ?? '' }),
  });
}
