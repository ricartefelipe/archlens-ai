'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import {
  FolderOpen,
  Settings,
  Users,
  Key,
  Webhook,
  Shield,
} from 'lucide-react';
import clsx from 'clsx';
import { UsagePanel } from '@/components/usage-panel';
import { UserSessionBar } from '@/components/user-session-bar';
import { AppLogo } from '@/components/app-logo';
import { APP_NAME, SUPPORT_URL } from '@/lib/branding';
import { isPlatformAdmin } from '@/lib/roles';

const mainNavItems = [
  { href: '/projects', label: 'Projetos', icon: FolderOpen },
];

const settingsNavItems = [
  { href: '/settings/team', label: 'Equipe', icon: Users },
  { href: '/settings/api-keys', label: 'Chaves API', icon: Key },
  { href: '/settings/webhooks', label: 'Webhooks', icon: Webhook },
];

const adminNavItems = [
  { href: '/admin/tenants', label: 'Contas', icon: Shield },
];

function NavLink({
  href,
  label,
  icon: Icon,
  pathname,
}: {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  pathname: string;
}) {
  const active = pathname.startsWith(href);

  return (
    <Link
      href={href}
      className={clsx(
        'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
        active
          ? 'bg-primary/10 text-primary'
          : 'text-muted-foreground hover:text-foreground hover:bg-secondary/50'
      )}
    >
      <Icon className="w-5 h-5" />
      {label}
    </Link>
  );
}

export function Sidebar() {
  const pathname = usePathname();
  const [platformAdmin, setPlatformAdmin] = useState(false);

  useEffect(() => {
    setPlatformAdmin(isPlatformAdmin());
  }, []);

  const settingsActive = pathname.startsWith('/settings');
  const adminActive = pathname.startsWith('/admin');

  return (
    <aside className="w-64 flex flex-col bg-background border-r border-border h-full">
      <div className="p-6 flex items-center gap-3">
        <AppLogo size="sm" />
        <span className="text-lg font-semibold text-foreground truncate">{APP_NAME}</span>
      </div>

      <nav className="flex-1 px-3 space-y-1 overflow-y-auto">
        {mainNavItems.map((item) => (
          <NavLink key={item.href} {...item} pathname={pathname} />
        ))}

        <div className="pt-4 pb-1 px-3">
          <Link
            href="/settings/team"
            className={clsx(
              'text-xs font-semibold uppercase tracking-wide flex items-center gap-2 rounded-lg px-3 py-2 -mx-0 transition-colors',
              settingsActive
                ? 'text-primary bg-primary/10'
                : 'text-muted-foreground hover:text-foreground hover:bg-secondary/50'
            )}
          >
            <Settings className="w-3.5 h-3.5" />
            Configurações
          </Link>
        </div>

        {settingsNavItems.map((item) => (
          <NavLink key={item.href} {...item} pathname={pathname} />
        ))}

        {platformAdmin && (
          <>
            <div className="pt-4 pb-1 px-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground flex items-center gap-2">
                <Shield className="w-3.5 h-3.5" />
                Admin
              </p>
            </div>
            {adminNavItems.map((item) => (
              <NavLink key={item.href} {...item} pathname={pathname} />
            ))}
          </>
        )}

        {(settingsActive || adminActive) && SUPPORT_URL && (
          <div className="pt-4 px-3">
            <a
              href={SUPPORT_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs text-muted-foreground hover:text-primary transition-colors"
            >
              Suporte
            </a>
          </div>
        )}
      </nav>

      <UsagePanel />
      <UserSessionBar />
    </aside>
  );
}
