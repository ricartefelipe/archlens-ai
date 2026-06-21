'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { acceptOrgInvite } from '@/lib/api';
import { getTenantId, login } from '@/lib/auth';
import { AppLogo } from '@/components/app-logo';
import { APP_NAME } from '@/lib/branding';

function InviteAcceptForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!token.trim()) {
      setError('Link de convite inválido — token ausente.');
      return;
    }
    if (!email.trim()) {
      return;
    }

    setLoading(true);
    setError('');
    try {
      const member = await acceptOrgInvite(getTenantId(), token.trim(), email.trim());
      if (member.tenantId) {
        login(member.tenantId);
      }
      setSuccess(true);
      window.setTimeout(() => {
        router.push('/projects');
      }, 1500);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao aceitar convite');
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3">
        Link de convite inválido. Solicite um novo convite ao administrador da organização.
      </p>
    );
  }

  if (success) {
    return (
      <div className="flex flex-col items-center gap-3 text-center py-4">
        <CheckCircle2 className="w-10 h-10 text-green-600" />
        <p className="text-sm text-foreground font-medium">Convite aceito com sucesso.</p>
        <p className="text-xs text-muted-foreground">Redirecionando para os projetos...</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-card border border-border rounded-xl p-6 space-y-4">
      <p className="text-sm text-muted-foreground">
        Informe o e-mail para o qual o convite foi enviado.
      </p>
      <div>
        <label className="block text-sm font-medium text-foreground mb-1.5">E-mail</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="voce@empresa.com"
          required
          className="w-full px-3 py-2.5 bg-background border border-border rounded-lg text-sm"
        />
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <button
        type="submit"
        disabled={loading}
        className="w-full py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50"
      >
        {loading ? 'Aceitando...' : 'Aceitar convite'}
      </button>
    </form>
  );
}

export default function InvitePage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-6">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <AppLogo size="md" className="mb-4" />
          <h1 className="text-2xl font-bold text-foreground">{APP_NAME}</h1>
          <p className="text-sm text-muted-foreground mt-1">Aceitar convite da equipe</p>
        </div>
        <Suspense
          fallback={
            <div className="flex justify-center py-8">
              <Loader2 className="w-8 h-8 text-primary animate-spin" />
            </div>
          }
        >
          <InviteAcceptForm />
        </Suspense>
      </div>
    </div>
  );
}
