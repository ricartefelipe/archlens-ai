import { expect, test } from '@playwright/test';
import { mockIds, setupMockApi } from '../fixtures/mock-api';

test.describe('Fluxo demo consultoria', () => {
  test.beforeEach(async ({ page }) => {
    await setupMockApi(page);
  });

  test('navega projeto → análise → chat com dados mockados', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/projects/);

    await expect(page.getByText('demo-loja')).toBeVisible();
    await page.getByText('demo-loja').click();
    await expect(page.getByRole('heading', { name: 'demo-loja' })).toBeVisible();

    await page.getByRole('button', { name: 'Arquivos (2)' }).click();
    await expect(page.getByText('openapi.yaml')).toBeVisible();

    await page.getByRole('button', { name: /Análises/ }).click();
    await page.getByText(/Análise estática concluída/).click();

    await expect(page.getByRole('heading', { name: 'Relatório de Análise' })).toBeVisible();
    await expect(page.getByText('SQL montado por concatenação de strings')).toBeVisible();
    await expect(page.getByText('Exportar Markdown')).toBeVisible();

    await page.goto(`/projects/${mockIds.PROJECT_ID}/chat?analysisId=${mockIds.ANALYSIS_ID}`);
    await expect(page.getByRole('heading', { name: 'Chat arquitetural' })).toBeVisible();

    await page.getByPlaceholder('Pergunte sobre riscos, API, Docker, migrations...').fill('Quais riscos devo priorizar?');
    await page.locator('form button[type="submit"]').click();

    await expect(page.getByText(/SQL concatenado|OrderController/i)).toBeVisible({ timeout: 10_000 });
  });
});
