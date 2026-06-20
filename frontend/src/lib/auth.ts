const TOKEN_KEY = 'archlens_access_token';
const TENANT_KEY = 'archlens_tenant';

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

export function isAuthenticated(): boolean {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem(TENANT_KEY);
}

function setSessionCookie(tenant: string): void {
  document.cookie = `archlens_tenant=${encodeURIComponent(tenant)}; path=/; max-age=604800; SameSite=Lax`;
}

function clearSessionCookie(): void {
  document.cookie = 'archlens_tenant=; path=/; max-age=0; SameSite=Lax';
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
  clearSessionCookie();
}

export function keycloakEnabled(): boolean {
  return !!process.env.NEXT_PUBLIC_KEYCLOAK_URL;
}

export async function loginWithKeycloak(username: string, password: string): Promise<void> {
  const baseUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL;
  const realm = process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'archlens';
  const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT || 'archlens-frontend';

  if (!baseUrl) {
    throw new Error('Keycloak não configurado');
  }

  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: clientId,
    username,
    password,
  });

  const res = await fetch(`${baseUrl}/realms/${realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });

  if (!res.ok) {
    throw new Error('Falha na autenticação');
  }

  const data = (await res.json()) as { access_token: string };
  const payload = JSON.parse(atob(data.access_token.split('.')[1] ?? '')) as {
    tenant_id?: string;
  };

  login(payload.tenant_id || 'default', data.access_token);
}
