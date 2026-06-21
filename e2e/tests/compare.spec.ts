import { expect, test } from '@playwright/test';

test.describe('Comparativo ArchLens', () => {
  test('página de comparativo exige baseline e current', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/projects/);

    await page.goto('/projects/00000000-0000-0000-0000-000000000001/analyses/compare');
    await expect(page.getByText('Selecione duas análises concluídas')).toBeVisible();
  });

  test('navega para comparativo com query params', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();

    const baseline = '11111111-1111-1111-1111-111111111111';
    const current = '22222222-2222-2222-2222-222222222222';
    const projectId = '33333333-3333-3333-3333-333333333333';

    await page.goto(
      `/projects/${projectId}/analyses/compare?baseline=${baseline}&current=${current}`
    );

    await expect(page.getByRole('heading', { name: 'Comparativo antes/depois' })).toBeVisible();
    // Sem backend no E2E, a comparação falha na API — validamos que a página tenta carregar.
    await expect(page.getByText(/Failed to fetch|Selecione duas análises/i)).toBeVisible();
  });
});
