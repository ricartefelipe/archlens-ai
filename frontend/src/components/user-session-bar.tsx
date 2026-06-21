'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { LogOut, User } from 'lucide-react';
import { getTenantId, getUserEmail, logout } from '@/lib/auth';

export function UserSessionBar() {
  const router = useRouter();
  const [email, setEmail] = useState<string | null>(null);
  const tenantId = getTenantId();

  useEffect(() => {
    setEmail(getUserEmail());
  }, []);

  function handleLogout() {
    logout();
    router.push('/login');
  }

  return (
    <div className="p-4 border-t border-border space-y-2">
      <div className="flex items-start gap-2 min-w-0">
        <User className="w-3.5 h-3.5 text-muted-foreground mt-0.5 shrink-0" />
        <div className="min-w-0 text-[11px] text-muted-foreground">
          {email && (
            <p className="text-foreground truncate" title={email}>
              {email}
            </p>
          )}
          <p className="truncate" title={tenantId}>
            Tenant: {tenantId}
          </p>
        </div>
      </div>
      <button
        type="button"
        onClick={handleLogout}
        className="flex w-full items-center justify-center gap-2 rounded-lg border border-border px-3 py-2 text-xs font-medium text-muted-foreground hover:text-foreground hover:bg-secondary/50 transition-colors"
      >
        <LogOut className="w-3.5 h-3.5" />
        Sair
      </button>
    </div>
  );
}
