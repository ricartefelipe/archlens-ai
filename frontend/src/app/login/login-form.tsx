'use client';

import { useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { isProductionAuth, login, loginWithPassword } from '@/lib/auth';
import { APP_NAME } from '@/lib/branding';
import { AppLogo } from '@/components/app-logo';

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = searchParams.get('next') || '/projects';

  const [tenantId, setTenantId] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const productionAuth = isProductionAuth();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (productionAuth) {
        await loginWithPassword(username, password);
      } else {
        const tenant = tenantId.trim() || 'default';
        login(tenant);
      }
      router.push(nextPath);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao entrar');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="w-full max-w-sm">
      <div className="flex flex-col items-center mb-8">
        <AppLogo size="md" className="mb-4" />
        <h1 className="text-2xl font-bold text-foreground">{APP_NAME}</h1>
        <p className="text-sm text-muted-foreground mt-1">Plataforma de diagnóstico arquitetural</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-card border border-border rounded-xl p-6 space-y-4">
        {productionAuth ? (
          <>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">E-mail</label>
              <input
                type="email"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                placeholder="seu@email.com"
                className="w-full px-3 py-2.5 bg-background border border-border rounded-lg text-sm"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Senha</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="w-full px-3 py-2.5 bg-background border border-border rounded-lg text-sm"
                required
              />
            </div>
          </>
        ) : (
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Tenant ID</label>
            <input
              type="text"
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
              placeholder="tenant-1"
              className="w-full px-3 py-2.5 bg-background border border-border rounded-lg text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            />
            <p className="text-xs text-muted-foreground mt-2">Modo desenvolvimento local — sem autenticação real.</p>
          </div>
        )}

        {error && <p className="text-sm text-destructive">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 transition-colors disabled:opacity-50"
        >
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
