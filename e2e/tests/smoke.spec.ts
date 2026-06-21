import { expect, test } from '@playwright/test';

test.describe('ArchLens smoke', () => {
  test('landing pública com CTA de login', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: /Evidências rastreáveis/i })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Acessar plataforma' })).toBeVisible();
  });

  test('exibe tela de login', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'ArchLens AI' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
  });

  test('redireciona rotas protegidas para login', async ({ page }) => {
    await page.goto('/projects');
    await expect(page).toHaveURL(/\/login/);
  });

  test('login dev define cookie de sessão', async ({ page, context }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/projects/);
    const cookies = await context.cookies();
    expect(cookies.some((c) => c.name === 'archlens_tenant')).toBeTruthy();
  });
});
