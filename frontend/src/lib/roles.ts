import { getAccessToken } from './auth';

interface JwtPayload {
  realm_roles?: string[] | string;
}

export function decodeJwtRoles(token?: string | null): string[] {
  const accessToken = token ?? getAccessToken();
  if (!accessToken) {
    return [];
  }

  try {
    const segment = accessToken.split('.')[1];
    if (!segment) {
      return [];
    }

    const payload = JSON.parse(atob(segment)) as JwtPayload;
    const roles = payload.realm_roles;

    if (Array.isArray(roles)) {
      return roles;
    }
    if (typeof roles === 'string') {
      return [roles];
    }
    return [];
  } catch {
    return [];
  }
}

export function isPlatformAdmin(roles?: string[]): boolean {
  return (roles ?? decodeJwtRoles()).includes('admin');
}

export function isOrgAdmin(roles?: string[]): boolean {
  const resolved = roles ?? decodeJwtRoles();
  return resolved.includes('ORG_ADMIN') || resolved.includes('admin');
}

export function hasWriteAccess(roles?: string[]): boolean {
  const resolved = roles ?? decodeJwtRoles();
  return (
    resolved.includes('admin') ||
    resolved.includes('architect') ||
    resolved.includes('ORG_ADMIN')
  );
}
