import { expect, test } from '@playwright/test';

test.describe('Configurações ArchLens', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/projects/);
  });

  test('página de equipe carrega', async ({ page }) => {
    await page.goto('/settings/team');
    await expect(page.getByRole('heading', { name: 'Equipe' })).toBeVisible();
    await expect(page.getByText('Gerencie membros e convites')).toBeVisible();
  });

  test('página de chaves API carrega', async ({ page }) => {
    await page.goto('/settings/api-keys');
    await expect(page.getByRole('heading', { name: /Chaves API/i })).toBeVisible();
  });

  test('página de webhooks carrega', async ({ page }) => {
    await page.goto('/settings/webhooks');
    await expect(page.getByRole('heading', { name: /Webhooks/i })).toBeVisible();
  });

  test('admin redireciona usuário dev para projetos', async ({ page }) => {
    await page.goto('/admin/tenants');
    await expect(page).toHaveURL(/\/projects/);
  });
});

test.describe('Convite ArchLens', () => {
  test('página de convite exige token na URL', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await page.goto('/invite');
    await expect(page.getByText(/Link de convite inválido/i)).toBeVisible();
  });

  test('página de convite com token exibe formulário', async ({ page }) => {
    await page.goto('/login?next=%2Finvite%3Ftoken%3Dtest-token-123');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/invite\?token=test-token-123/);
    await expect(page.getByText('Aceitar convite da equipe')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Aceitar convite' })).toBeVisible();
  });
});
