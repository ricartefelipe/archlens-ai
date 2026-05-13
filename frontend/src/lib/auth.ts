export function getTenantId(): string {
  if (typeof window === 'undefined') return 'default';
  return localStorage.getItem('archlens_tenant') || 'default';
}

export function setTenantId(tenant: string): void {
  localStorage.setItem('archlens_tenant', tenant);
}

export function isAuthenticated(): boolean {
  return typeof window !== 'undefined' && !!localStorage.getItem('archlens_tenant');
}

export function login(tenant: string): void {
  setTenantId(tenant);
}

export function logout(): void {
  if (typeof window !== 'undefined') localStorage.removeItem('archlens_tenant');
}
