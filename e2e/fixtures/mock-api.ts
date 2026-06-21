import type { Page, Route } from '@playwright/test';

const PROJECT_ID = '11111111-1111-4111-8111-111111111111';
const ANALYSIS_ID = '22222222-2222-4222-8222-222222222222';
const TENANT = 'tenant-1';

const now = new Date().toISOString();

const project = {
  id: PROJECT_ID,
  tenantId: TENANT,
  name: 'demo-loja',
  description: 'Sample ArchLens — loja fictícia',
  status: 'READY',
  fileCount: 3,
  createdAt: now,
  updatedAt: now,
};

const files = [
  {
    id: 'f1',
    projectId: PROJECT_ID,
    filePath: 'openapi.yaml',
    fileType: 'OPENAPI',
    sizeBytes: 420,
    contentHash: 'abc',
    createdAt: now,
  },
  {
    id: 'f2',
    projectId: PROJECT_ID,
    filePath: 'Dockerfile',
    fileType: 'DOCKER',
    sizeBytes: 180,
    contentHash: 'def',
    createdAt: now,
  },
];

const risks = [
  {
    id: 'r1',
    category: 'SECURITY_RISK',
    severity: 'CRITICAL',
    title: 'SQL montado por concatenação de strings',
    description: 'Queries JDBC com concatenação expõem SQL injection.',
    filePath: 'src/main/java/com/demo/loja/OrderController.java',
    evidence: 'createStatement().execute(... + ...)',
    suggestion: 'Usar PreparedStatement com placeholders',
  },
  {
    id: 'r2',
    category: 'CONTRACT_VIOLATION',
    severity: 'MEDIUM',
    title: 'OpenAPI sem respostas de erro',
    description: 'Operações sem 4xx/5xx documentadas.',
    filePath: 'openapi.yaml',
    evidence: '3 endpoint(s) sem 4xx/5xx',
    suggestion: 'Adicionar respostas 400, 404 e 500',
  },
];

const completedAnalysis = {
  id: ANALYSIS_ID,
  projectId: PROJECT_ID,
  status: 'COMPLETED',
  summary: 'Análise estática concluída. 2 riscos identificados.',
  risks,
  createdAt: now,
  updatedAt: now,
};

const usage = {
  tenantId: TENANT,
  plan: 'DIAGNOSTICO',
  planDisplayName: 'Diagnóstico',
  status: 'ACTIVE',
  projectsUsed: 1,
  projectsLimit: 10,
  analysesUsed: 1,
  analysesLimit: 100,
  uploadMbUsed: 1,
  uploadMbLimit: 500,
  usagePeriodStart: now,
};

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

export async function setupMockApi(page: Page) {
  await page.route('**/v1/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();

    if (path === '/v1/account/usage' && method === 'GET') {
      return json(route, usage);
    }

    if (path === '/v1/projects' && method === 'GET') {
      return json(route, [project]);
    }

    if (path === `/v1/projects/${PROJECT_ID}` && method === 'GET') {
      return json(route, project);
    }

    if (path === `/v1/projects/${PROJECT_ID}/files` && method === 'GET') {
      return json(route, files);
    }

    if (path === `/v1/projects/${PROJECT_ID}/analyses` && method === 'GET') {
      return json(route, [completedAnalysis]);
    }

    if (path === `/v1/projects/${PROJECT_ID}/analyses` && method === 'POST') {
      return json(route, { ...completedAnalysis, status: 'PENDING', risks: [], summary: null });
    }

    if (path === `/v1/projects/${PROJECT_ID}/analyses/${ANALYSIS_ID}` && method === 'GET') {
      return json(route, completedAnalysis);
    }

    if (
      path === `/v1/projects/${PROJECT_ID}/analyses/${ANALYSIS_ID}/questions`
      && method === 'GET'
    ) {
      return json(route, []);
    }

    if (
      path === `/v1/projects/${PROJECT_ID}/analyses/${ANALYSIS_ID}/questions`
      && method === 'POST'
    ) {
      const payload = route.request().postDataJSON() as { question?: string };
      return json(route, {
        id: 'q1',
        analysisId: ANALYSIS_ID,
        question: payload.question ?? '',
        answer: 'Com base nos achados, priorize corrigir SQL concatenado no OrderController e documentar erros no OpenAPI.',
        sources: null,
        createdAt: now,
      });
    }

    return json(route, { message: 'not mocked' }, 404);
  });
}

export const mockIds = { PROJECT_ID, ANALYSIS_ID };
