import { describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import { proxy } from './proxy';

function request(path: string, cookie?: string) {
  const headers = new Headers();
  if (cookie) {
    headers.set('cookie', cookie);
  }
  return new NextRequest(new URL(path, 'http://localhost:3000'), { headers });
}

describe('proxy — proteção de rotas de página', () => {
  it('redireciona rota protegida sem cookie de tenant para /login com next', () => {
    const res = proxy(request('/projects/abc?tab=analyses'));

    expect(res.status).toBe(307);
    const location = new URL(res.headers.get('location') ?? '');
    expect(location.pathname).toBe('/login');
    expect(location.searchParams.get('next')).toBe('/projects/abc?tab=analyses');
  });

  it('permite rota protegida com cookie de tenant', () => {
    const res = proxy(request('/projects', 'archlens_tenant=tenant-1'));
    expect(res.headers.get('location')).toBeNull();
  });

  it('mantém a raiz e o login públicos', () => {
    expect(proxy(request('/')).headers.get('location')).toBeNull();
    expect(proxy(request('/login')).headers.get('location')).toBeNull();
  });
});

describe('proxy — rotas de API', () => {
  // A UI chama a API na mesma origem; redirecionar essas rotas para /login
  // transformava o POST de login em 307 seguido de 405 na página.
  it.each([
    '/public/auth/login',
    '/public/auth/refresh',
    '/v1/projects',
    '/v1/projects/abc/analyses/def/report',
    '/q/health/ready',
  ])('não redireciona %s mesmo sem cookie', (path) => {
    const res = proxy(request(path));
    expect(res.status).toBe(200);
    expect(res.headers.get('location')).toBeNull();
  });

  it('não confunde páginas cujo nome começa com um prefixo de API', () => {
    // "/v1x" não é a API; segue a proteção normal de páginas.
    const res = proxy(request('/v1x'));
    expect(res.status).toBe(307);
  });
});
