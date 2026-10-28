import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

import { isApiPath } from '@/lib/api-routes';

// Prefixos públicos (casam o caminho e seus sub-caminhos).
const PUBLIC_PREFIXES = ['/login', '/auth/callback'];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Chamadas de API são encaminhadas ao backend (ver next.config.ts), que faz a
  // autorização por JWT. Redirecioná-las para /login transformaria o POST de
  // login em 307 seguido de 405 na página.
  if (isApiPath(pathname)) {
    return NextResponse.next();
  }

  // A raiz "/" é pública apenas como correspondência exata; usar startsWith('/')
  // tornaria todas as rotas públicas e desativaria a proteção.
  if (pathname === '/' || PUBLIC_PREFIXES.some((path) => pathname.startsWith(path))) {
    return NextResponse.next();
  }

  const tenant = request.cookies.get('archlens_tenant')?.value;
  if (!tenant) {
    const loginUrl = request.nextUrl.clone();
    loginUrl.pathname = '/login';
    const next = `${pathname}${request.nextUrl.search}`;
    loginUrl.searchParams.set('next', next);
    return NextResponse.redirect(loginUrl);
  }

  // Rotas /admin/* exigem autenticação; autorização de platform admin é feita no layout client-side.
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
