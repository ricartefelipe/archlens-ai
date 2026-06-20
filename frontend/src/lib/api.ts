import type { Project, ProjectFile, Analysis, Adr, Question, AccountUsage } from './types';
import { getAccessToken } from './auth';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

function correlationId(): string {
  return crypto.randomUUID();
}

async function apiFetch<T>(path: string, tenantId: string, options: RequestInit = {}): Promise<T> {
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

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`API ${res.status}: ${text || res.statusText}`);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}

export async function listProjects(tenantId: string): Promise<Project[]> {
  return apiFetch('/v1/projects', tenantId);
}

export async function getAccountUsage(tenantId: string): Promise<AccountUsage> {
  return apiFetch('/v1/account/usage', tenantId);
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
  return `${API_BASE}/v1/projects/${projectId}/analyses/${analysisId}/report?format=${format}`;
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
