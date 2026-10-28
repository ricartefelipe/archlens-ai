import { expect, test } from '@playwright/test';

/**
 * Suíte de integração REAL: roda contra a stack completa (frontend buildado +
 * API + worker + Postgres + Keycloak), com autenticação real e os dados do
 * seed demo-loja (`scripts/seed-demo-loja.sh`).
 *
 * Só executa quando E2E_REAL=1; a suíte padrão (mockada, frontend dev) cobre a
 * CI. Pré-requisitos: stack no ar e seed executado.
 *
 *   E2E_REAL=1 E2E_SKIP_SERVER=1 \
 *   E2E_BASE_URL=http://localhost:3000 E2E_REAL_API=http://localhost:8080 \
 *   npx playwright test tests/integration-real.spec.ts
 */
const REAL = process.env.E2E_REAL === '1';
const API = process.env.E2E_REAL_API ?? 'http://localhost:8080';
const EMAIL = process.env.ARCHLENS_EMAIL ?? 'architect@archlens.dev';
const PASSWORD = process.env.ARCHLENS_PASSWORD ?? 'arch123';
const TENANT = process.env.ARCHLENS_TENANT ?? 'tenant-1';

test.describe('Integração real (stack completa)', () => {
  test.skip(!REAL, 'Defina E2E_REAL=1 para rodar contra a stack real');

  // Serial de propósito: logins simultâneos com a mesma conta disparam a
  // proteção contra força bruta do Keycloak e o teste falharia por isso.
  test.describe.configure({ mode: 'serial' });

  async function loginViaUi(page: import('@playwright/test').Page) {
    await page.goto('/login');
    await page.getByPlaceholder('seu@email.com').fill(EMAIL);
    await page.locator('input[type="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/projects/, { timeout: 15_000 });
  }

  test('rota protegida sem sessão redireciona para login', async ({ page }) => {
    await page.goto('/projects');
    await expect(page).toHaveURL(/\/login/);
  });

  test('login real, projeto semeado e análise concluída visíveis', async ({ page }) => {
    await loginViaUi(page);
    const projectLink = page.getByText('demo-loja').first();
    await expect(projectLink).toBeVisible({ timeout: 15_000 });
    await projectLink.click();

    await expect(page.getByRole('heading', { name: 'demo-loja' })).toBeVisible({ timeout: 20_000 });
    await page.getByRole('button', { name: /Análises \(\d+\)/ }).click();
    await expect(page.getByText(/Conclu|COMPLETED/i).first()).toBeVisible({ timeout: 20_000 });
  });

  test('comparação real entre duas análises concluídas', async ({ page, request }) => {
    // IDs reais via API (mesma credencial), para abrir o comparativo na UI.
    const login = await request.post(`${API}/public/auth/login`, {
      data: { email: EMAIL, password: PASSWORD },
    });
    expect(login.ok()).toBeTruthy();
    const { accessToken } = (await login.json()) as { accessToken: string };
    const headers = { Authorization: `Bearer ${accessToken}`, 'X-Tenant-Id': TENANT };

    const projects = (await (await request.get(`${API}/v1/projects`, { headers })).json()) as Array<{
      id: string;
      name: string;
    }>;
    const project = projects.find((p) => p.name === 'demo-loja');
    expect(project, 'projeto demo-loja deve existir (rodar seed antes)').toBeTruthy();

    const analyses = (await (
      await request.get(`${API}/v1/projects/${project!.id}/analyses`, { headers })
    ).json()) as Array<{ id: string; status: string }>;
    const completed = analyses.filter((a) => a.status === 'COMPLETED');
    test.skip(completed.length < 2, 'comparação exige duas análises concluídas');

    await loginViaUi(page);
    await page.goto(
      `/projects/${project!.id}/analyses/compare?baseline=${completed[0].id}&current=${completed[1].id}`
    );
    await expect(page.getByRole('heading', { name: 'Comparativo antes/depois' })).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByRole('button', { name: /Exportar Markdown/i })).toBeVisible();
  });

  test('comparação inexistente mostra erro na UI sem detalhes internos', async ({ page }) => {
    await loginViaUi(page);
    await page.goto(
      '/projects/00000000-0000-0000-0000-000000000001/analyses/compare?baseline=00000000-0000-0000-0000-000000000002&current=00000000-0000-0000-0000-000000000003'
    );
    // A UI deve exibir um estado de erro legível, sem stack trace nem SQL.
    const message = page.getByText(/não encontrad|not found|API \d{3}|Selecione duas análises/i).first();
    await expect(message).toBeVisible({ timeout: 20_000 });
    const text = (await message.innerText()).toLowerCase();
    expect(text).not.toContain('exception');
    expect(text).not.toContain('select ');
    expect(text).not.toContain('/data/');
  });
});
