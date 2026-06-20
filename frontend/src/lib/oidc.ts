import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

import { login } from './auth';

const KEYCLOAK_URL = process.env.NEXT_PUBLIC_KEYCLOAK_URL ?? '';
const REALM = process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? 'archlens';
const CLIENT_ID = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT ?? 'archlens-frontend';

export function oidcEnabled(): boolean {
  return process.env.NEXT_PUBLIC_OIDC_ENABLED === 'true' && !!KEYCLOAK_URL;
}

function createUserManager(): UserManager {
  const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000';
  return new UserManager({
    authority: `${KEYCLOAK_URL}/realms/${REALM}`,
    client_id: CLIENT_ID,
    redirect_uri: `${origin}/auth/callback`,
    post_logout_redirect_uri: `${origin}/login`,
    response_type: 'code',
    scope: 'openid profile email',
    automaticSilentRenew: true,
    userStore: new WebStorageStateStore({ store: window.localStorage }),
  });
}

let userManager: UserManager | null = null;

function getUserManager(): UserManager {
  if (!userManager) {
    userManager = createUserManager();
  }
  return userManager;
}

export async function signInWithPkce(): Promise<void> {
  await getUserManager().signinRedirect();
}

export async function handleSignInCallback(): Promise<User> {
  return getUserManager().signinRedirectCallback();
}

export async function signOutOidc(): Promise<void> {
  await getUserManager().signoutRedirect();
}

export async function getOidcUser(): Promise<User | null> {
  if (!oidcEnabled()) {
    return null;
  }
  return getUserManager().getUser();
}

export function applyOidcUser(user: User): void {
  const tenant = (user.profile.tenant_id as string | undefined) ?? 'default';
  login(tenant, user.access_token);
}
