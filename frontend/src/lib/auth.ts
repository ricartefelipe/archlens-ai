import { getApiBase } from './api-base';

const TOKEN_KEY = 'archlens_access_token';
const REFRESH_KEY = 'archlens_refresh_token';
const TENANT_KEY = 'archlens_tenant';

type SessionPayload = {
  accessToken: string;
  refreshToken?: string | null;
  tenantId: string;
};

export function getTenantId(): string {
  if (typeof window === 'undefined') return 'default';
  return localStorage.getItem(TENANT_KEY) || 'default';
}

export function setTenantId(tenant: string): void {
  localStorage.setItem(TENANT_KEY, tenant);
}

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_KEY, token);
}

export function isAuthenticated(): boolean {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem(TENANT_KEY) && !!localStorage.getItem(TOKEN_KEY);
}

function setSessionCookie(tenant: string): void {
  document.cookie = `archlens_tenant=${encodeURIComponent(tenant)}; path=/; max-age=604800; SameSite=Lax`;
}

function clearSessionCookie(): void {
  document.cookie = 'archlens_tenant=; path=/; max-age=0; SameSite=Lax';
}

function persistSession(data: SessionPayload): void {
  setTenantId(data.tenantId);
  setSessionCookie(data.tenantId);
  setAccessToken(data.accessToken);
  if (data.refreshToken) {
    setRefreshToken(data.refreshToken);
  }
}

export function login(tenant: string, accessToken?: string): void {
  setTenantId(tenant);
  setSessionCookie(tenant);
  if (accessToken) {
    setAccessToken(accessToken);
  }
}

export function logout(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TENANT_KEY);
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  clearSessionCookie();
}

/** E-mail ou usuário do JWT de sessão (piloto BFF). */
export function getUserEmail(): string | null {
  const token = getAccessToken();
  if (!token) {
    return null;
  }
  try {
    const segment = token.split('.')[1];
    if (!segment) {
      return null;
    }
    const payload = JSON.parse(atob(segment)) as {
      email?: string;
      preferred_username?: string;
    };
    return payload.email || payload.preferred_username || null;
  } catch {
    return null;
  }
}

/** Produção/piloto: login via API BFF (Keycloak transparente ao usuário). */
export function isProductionAuth(): boolean {
  return process.env.NEXT_PUBLIC_AUTH_MODE === 'bff'
    || (process.env.NEXT_PUBLIC_OIDC_ENABLED !== 'true' && !!process.env.NEXT_PUBLIC_API_URL);
}

export async function refreshSession(): Promise<boolean> {
  if (typeof window === 'undefined') {
    return false;
  }

  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (!refreshToken) {
    return false;
  }

  const res = await fetch(`${getApiBase()}/public/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    return false;
  }

  const data = (await res.json()) as SessionPayload;
  persistSession(data);
  return true;
}

export async function loginWithPassword(email: string, password: string): Promise<void> {
  const res = await fetch(`${getApiBase()}/public/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: email.trim(), password }),
  });

  if (!res.ok) {
    let message = 'E-mail ou senha inválidos';
    try {
      const body = (await res.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // mantém mensagem padrão
    }
    throw new Error(message);
  }

  const data = (await res.json()) as SessionPayload;
  persistSession(data);
}

export function redirectToLogin(): void {
  logout();
  window.location.href = '/login';
}
