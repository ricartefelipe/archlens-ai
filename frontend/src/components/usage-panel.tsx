'use client';

import { useEffect, useState } from 'react';
import { Gauge, ExternalLink } from 'lucide-react';
import { getTenantId } from '@/lib/auth';
import { getAccountUsage } from '@/lib/api';
import type { AccountUsage } from '@/lib/types';

const LANDING_URL = process.env.NEXT_PUBLIC_LANDING_URL ?? 'https://archlens.dev';

function formatLimit(used: number, limit: number): string {
  if (limit < 0) {
    return `${used} / ilimitado`;
  }
  return `${used} / ${limit}`;
}

function usagePercent(used: number, limit: number): number {
  if (limit <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((used / limit) * 100));
}

export function UsagePanel() {
  const tenantId = getTenantId();
  const [usage, setUsage] = useState<AccountUsage | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getAccountUsage(tenantId)
      .then((data) => {
        if (!cancelled) {
          setUsage(data);
          setError(null);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [tenantId]);

  if (error) {
    return (
      <div className="p-4 border-t border-border text-xs text-muted-foreground">
        Uso comercial indisponível
      </div>
    );
  }

  if (!usage) {
    return (
      <div className="p-4 border-t border-border text-xs text-muted-foreground animate-pulse">
        Carregando plano…
      </div>
    );
  }

  const analysesPct = usagePercent(usage.analysesUsed, usage.analysesLimit);

  return (
    <div className="p-4 border-t border-border space-y-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-medium text-foreground">
          <Gauge className="w-3.5 h-3.5 text-primary" />
          <span>{usage.planDisplayName}</span>
        </div>
        <a
          href={LANDING_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 text-[10px] text-primary hover:underline"
        >
          Upgrade
          <ExternalLink className="w-3 h-3" />
        </a>
      </div>

      <div className="space-y-2 text-[11px] text-muted-foreground">
        <div>
          <div className="flex justify-between mb-0.5">
            <span>Projetos</span>
            <span>{formatLimit(usage.projectsUsed, usage.projectsLimit)}</span>
          </div>
        </div>
        <div>
          <div className="flex justify-between mb-0.5">
            <span>Análises (mês)</span>
            <span>{formatLimit(usage.analysesUsed, usage.analysesLimit)}</span>
          </div>
          {usage.analysesLimit > 0 && (
            <div className="h-1 rounded-full bg-secondary overflow-hidden">
              <div
                className="h-full bg-primary transition-all"
                style={{ width: `${analysesPct}%` }}
              />
            </div>
          )}
        </div>
        <div className="flex justify-between">
          <span>Upload (mês)</span>
          <span>{formatLimit(usage.uploadMbUsed, usage.uploadMbLimit)} MB</span>
        </div>
      </div>
    </div>
  );
}
