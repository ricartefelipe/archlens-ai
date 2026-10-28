'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { Sidebar } from '@/components/sidebar';
import { isPlatformAdmin } from '@/lib/roles';
import { useClientValue } from '@/lib/use-client-value';

export default function AdminShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  // null durante SSR/hidratação; booleano real no cliente, sem setState em effect.
  const authorized = useClientValue<boolean | null>(isPlatformAdmin, null);

  useEffect(() => {
    if (authorized === false) {
      router.replace('/projects');
    }
  }, [authorized, router]);

  if (authorized === null) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    );
  }

  if (!authorized) {
    return null;
  }

  return (
    <div className="flex h-screen bg-background">
      <Sidebar />
      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}
