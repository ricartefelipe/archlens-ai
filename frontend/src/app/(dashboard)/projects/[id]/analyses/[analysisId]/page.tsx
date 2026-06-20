'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { Loader2, AlertTriangle, ShieldAlert, ShieldCheck, Shield, Download } from 'lucide-react';
import { format } from 'date-fns';
import { getAnalysis, listAdrs, downloadReport } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { StatusBadge } from '@/components/status-badge';
import { RiskCard } from '@/components/risk-card';
import type { Analysis, Adr } from '@/lib/types';

const severityOrder: Record<string, number> = {
  CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3,
};

const severityIcons: Record<string, typeof ShieldAlert> = {
  CRITICAL: ShieldAlert,
  HIGH: AlertTriangle,
  MEDIUM: Shield,
  LOW: ShieldCheck,
};

const severityCardColors: Record<string, string> = {
  CRITICAL: 'border-red-500/30 bg-red-500/10 text-red-400',
  HIGH: 'border-orange-500/30 bg-orange-500/10 text-orange-400',
  MEDIUM: 'border-yellow-500/30 bg-yellow-500/10 text-yellow-400',
  LOW: 'border-green-500/30 bg-green-500/10 text-green-400',
};

export default function AnalysisDetailPage() {
  const params = useParams();
  const projectId = params.id as string;
  const analysisId = params.analysisId as string;

  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [adrs, setAdrs] = useState<Adr[]>([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState<'markdown' | 'json' | 'pdf' | null>(null);

  const loadData = useCallback(async () => {
    const tenantId = getTenantId();
    try {
      const [a, adrList] = await Promise.all([
        getAnalysis(tenantId, projectId, analysisId),
        listAdrs(tenantId, projectId, analysisId).catch(() => []),
      ]);
      setAnalysis(a);
      setAdrs(adrList);
    } catch (err) {
      console.error('Failed to load analysis:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId, analysisId]);

  useEffect(() => {
    const t = window.setTimeout(() => {
      void loadData();
    }, 0);
    return () => window.clearTimeout(t);
  }, [loadData]);

  useEffect(() => {
    if (!analysis) return;
    if (analysis.status !== 'PENDING' && analysis.status !== 'PROCESSING') return;

    const interval = setInterval(async () => {
      try {
        const updated = await getAnalysis(getTenantId(), projectId, analysisId);
        setAnalysis(updated);
        if (updated.status === 'COMPLETED' || updated.status === 'FAILED') {
          const adrList = await listAdrs(getTenantId(), projectId, analysisId).catch(() => []);
          setAdrs(adrList);
          clearInterval(interval);
        }
      } catch { /* retry next tick */ }
    }, 3000);

    return () => clearInterval(interval);
  }, [analysis?.status, projectId, analysisId]); // eslint-disable-line react-hooks/exhaustive-deps -- evita restart do interval em cada objeto analysis novo

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    );
  }

  if (!analysis) {
    return <div className="p-8 text-destructive">Analysis not found</div>;
  }

  const sortedRisks = [...analysis.risks].sort(
    (a, b) => (severityOrder[a.severity] ?? 9) - (severityOrder[b.severity] ?? 9)
  );

  const riskCounts = analysis.risks.reduce<Record<string, number>>((acc, r) => {
    acc[r.severity] = (acc[r.severity] || 0) + 1;
    return acc;
  }, {});

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div>
        <div className="flex items-center gap-3 mb-2">
          <h1 className="text-2xl font-bold text-foreground">Relatório de Análise</h1>
          <StatusBadge status={analysis.status} size="md" />
          {analysis.status === 'COMPLETED' && (
            <div className="ml-auto flex gap-2">
              <button
                type="button"
                disabled={!!exporting}
                onClick={async () => {
                  setExporting('markdown');
                  try {
                    await downloadReport(getTenantId(), projectId, analysisId, 'markdown');
                  } finally {
                    setExporting(null);
                  }
                }}
                className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-border hover:bg-muted transition-colors disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                {exporting === 'markdown' ? 'Exportando...' : 'Exportar Markdown'}
              </button>
              <button
                type="button"
                disabled={!!exporting}
                onClick={async () => {
                  setExporting('json');
                  try {
                    await downloadReport(getTenantId(), projectId, analysisId, 'json');
                  } finally {
                    setExporting(null);
                  }
                }}
                className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-border hover:bg-muted transition-colors disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                {exporting === 'json' ? 'Exportando...' : 'Exportar JSON'}
              </button>
              <button
                type="button"
                disabled={!!exporting}
                onClick={async () => {
                  setExporting('pdf');
                  try {
                    await downloadReport(getTenantId(), projectId, analysisId, 'pdf');
                  } finally {
                    setExporting(null);
                  }
                }}
                className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-border hover:bg-muted transition-colors disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                {exporting === 'pdf' ? 'Exportando...' : 'Exportar PDF'}
              </button>
            </div>
          )}
        </div>
        <div className="flex gap-4 text-xs text-muted-foreground">
          <span>Created: {format(new Date(analysis.createdAt), 'MMM d, yyyy HH:mm')}</span>
          <span>Updated: {format(new Date(analysis.updatedAt), 'MMM d, yyyy HH:mm')}</span>
        </div>
        {analysis.summary && (
          <p className="mt-3 text-sm text-foreground bg-card rounded-xl border border-border p-4">
            {analysis.summary}
          </p>
        )}
      </div>

      {(analysis.status === 'PENDING' || analysis.status === 'PROCESSING') && (
        <div className="flex items-center gap-3 bg-primary/10 rounded-xl p-6">
          <Loader2 className="w-6 h-6 text-primary animate-spin" />
          <p className="text-sm text-foreground">Analysis is being processed...</p>
        </div>
      )}

      {analysis.risks.length > 0 && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const).map((sev) => {
              const Icon = severityIcons[sev];
              const count = riskCounts[sev] || 0;
              return (
                <div key={sev} className={`rounded-xl border p-4 ${severityCardColors[sev]}`}>
                  <div className="flex items-center gap-2 mb-1">
                    <Icon className="w-5 h-5" />
                    <span className="text-sm font-medium">{sev}</span>
                  </div>
                  <p className="text-3xl font-bold">{count}</p>
                </div>
              );
            })}
          </div>

          <div>
            <h2 className="text-lg font-semibold text-foreground mb-4">
              Risk Details ({sortedRisks.length})
            </h2>
            <div className="space-y-2">
              {sortedRisks.map((risk) => (
                <RiskCard key={risk.id} risk={risk} />
              ))}
            </div>
          </div>
        </>
      )}

      {adrs.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-foreground mb-4">
            Architectural Decision Records ({adrs.length})
          </h2>
          <div className="grid gap-4">
            {adrs.map((adr) => (
              <div key={adr.id} className="bg-card border border-border rounded-xl p-5 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-semibold text-foreground">{adr.title}</h3>
                  <StatusBadge status={adr.status} />
                </div>
                <div className="space-y-2 text-sm">
                  <div>
                    <p className="text-xs text-muted-foreground font-medium mb-0.5">Context</p>
                    <p className="text-foreground">{adr.context}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground font-medium mb-0.5">Decision</p>
                    <p className="text-foreground">{adr.decision}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground font-medium mb-0.5">Consequences</p>
                    <p className="text-foreground">{adr.consequences}</p>
                  </div>
                </div>
                {adr.relatedFindings?.length > 0 && (
                  <p className="text-xs text-muted-foreground">
                    {adr.relatedFindings.length} related findings
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
