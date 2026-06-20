'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { applyOidcUser, handleSignInCallback } from '@/lib/oidc';

export default function AuthCallbackPage() {
  const router = useRouter();
  const [error, setError] = useState('');

  useEffect(() => {
    void handleSignInCallback()
      .then((user) => {
        applyOidcUser(user);
        router.replace('/projects');
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : 'Falha na autenticação OIDC');
      });
  }, [router]);

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <p className="text-sm text-muted-foreground">Concluindo autenticação...</p>
    </div>
  );
}
