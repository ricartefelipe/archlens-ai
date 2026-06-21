'use client';

import { ArrowDownRight, ArrowUpRight, Minus, TrendingDown, TrendingUp } from 'lucide-react';
import clsx from 'clsx';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { RiskCard } from '@/components/risk-card';
import type { AnalysisComparison, ArchitecturalRisk } from '@/lib/types';
import { severityLabel } from '@/lib/labels';

const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const;

const severityColors: Record<string, string> = {
  CRITICAL: 'text-red-400',
  HIGH: 'text-orange-400',
  MEDIUM: 'text-yellow-400',
  LOW: 'text-green-400',
};

interface AnalysisComparisonViewProps {
  comparison: AnalysisComparison;
}

function SeverityDeltaCards({ comparison }: AnalysisComparisonViewProps) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
      {severities.map((severity) => {
        const before = comparison.baselineSeverityCounts[severity] ?? 0;
        const after = comparison.currentSeverityCounts[severity] ?? 0;
        const delta = after - before;
        return (
          <div key={severity} className="bg-card border border-border rounded-xl p-4">
            <p className={clsx('text-xs font-semibold mb-2', severityColors[severity])}>
              {severityLabel(severity)}
            </p>
            <div className="flex items-end justify-between gap-2">
              <div>
                <p className="text-xs text-muted-foreground">Antes</p>
                <p className="text-lg font-bold text-foreground">{before}</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">Depois</p>
                <p className="text-lg font-bold text-foreground">{after}</p>
              </div>
            </div>
            <p className={clsx(
              'text-xs mt-2 flex items-center gap-1',
              delta > 0 ? 'text-red-400' : delta < 0 ? 'text-green-400' : 'text-muted-foreground'
            )}>
              {delta > 0 ? <TrendingUp className="w-3 h-3" /> : delta < 0 ? <TrendingDown className="w-3 h-3" /> : <Minus className="w-3 h-3" />}
              {delta === 0 ? 'sem alteração' : `${delta > 0 ? '+' : ''}${delta}`}
            </p>
          </div>
        );
      })}
    </div>
  );
}

function RiskSection({
  title,
  icon: Icon,
  risks,
  tone,
}: {
  title: string;
  icon: typeof ArrowUpRight;
  risks: ArchitecturalRisk[];
  tone: 'added' | 'removed' | 'neutral';
}) {
  if (risks.length === 0) {
    return null;
  }

  const borderTone = {
    added: 'border-red-500/30',
    removed: 'border-green-500/30',
    neutral: 'border-border',
  }[tone];

  return (
    <section className={clsx('bg-card border rounded-xl p-4', borderTone)}>
      <div className="flex items-center gap-2 mb-3">
        <Icon className={clsx('w-4 h-4', tone === 'added' ? 'text-red-400' : tone === 'removed' ? 'text-green-400' : 'text-muted-foreground')} />
        <h3 className="text-sm font-semibold text-foreground">{title}</h3>
        <span className="text-xs text-muted-foreground">({risks.length})</span>
      </div>
      <div className="space-y-2">
        {risks.map((risk) => (
          <RiskCard key={risk.id} risk={risk} />
        ))}
      </div>
    </section>
  );
}

export function AnalysisComparisonView({ comparison }: AnalysisComparisonViewProps) {
  const netDelta =
    comparison.current.riskCount - comparison.baseline.riskCount;

  return (
    <div className="space-y-6">
      <div className="grid md:grid-cols-2 gap-4">
        <div className="bg-card border border-border rounded-xl p-4">
          <p className="text-xs uppercase tracking-wide text-muted-foreground mb-1">Referência (antes)</p>
          <p className="text-sm font-medium text-foreground">
            {format(new Date(comparison.baseline.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
          </p>
          <p className="text-xs text-muted-foreground mt-1">{comparison.baseline.riskCount} riscos</p>
        </div>
        <div className="bg-card border border-primary/30 rounded-xl p-4">
          <p className="text-xs uppercase tracking-wide text-primary mb-1">Atual (depois)</p>
          <p className="text-sm font-medium text-foreground">
            {format(new Date(comparison.current.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {comparison.current.riskCount} riscos
            <span className={clsx('ml-2', netDelta > 0 ? 'text-red-400' : netDelta < 0 ? 'text-green-400' : '')}>
              ({netDelta > 0 ? '+' : ''}{netDelta} total)
            </span>
          </p>
        </div>
      </div>

      <SeverityDeltaCards comparison={comparison} />

      <RiskSection
        title="Novos riscos"
        icon={ArrowUpRight}
        risks={comparison.added}
        tone="added"
      />

      <RiskSection
        title="Riscos resolvidos"
        icon={ArrowDownRight}
        risks={comparison.removed}
        tone="removed"
      />

      {comparison.severityChanged.length > 0 && (
        <section className="bg-card border border-yellow-500/30 rounded-xl p-4">
          <h3 className="text-sm font-semibold text-foreground mb-3">
            Severidade alterada ({comparison.severityChanged.length})
          </h3>
          <div className="space-y-3">
            {comparison.severityChanged.map((change) => (
              <div key={change.currentRisk.id} className="border border-border rounded-lg p-3">
                <p className="text-sm font-medium text-foreground mb-2">{change.currentRisk.title}</p>
                <div className="flex items-center gap-2 text-xs">
                  <span className="text-muted-foreground">{severityLabel(change.baselineRisk.severity)}</span>
                  <span className="text-muted-foreground">→</span>
                  <span className={severityColors[change.currentRisk.severity]}>
                    {severityLabel(change.currentRisk.severity)}
                  </span>
                  <span className="text-muted-foreground truncate">{change.currentRisk.filePath}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {comparison.unchanged.length > 0 && (
        <details className="bg-card border border-border rounded-xl p-4">
          <summary className="text-sm font-semibold text-foreground cursor-pointer">
            Inalterados ({comparison.unchanged.length})
          </summary>
          <div className="space-y-2 mt-3">
            {comparison.unchanged.map((risk) => (
              <RiskCard key={risk.id} risk={risk} />
            ))}
          </div>
        </details>
      )}
    </div>
  );
}
