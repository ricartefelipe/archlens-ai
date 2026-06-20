'use client';

import { Suspense, useEffect, useState } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft, Download, Loader2 } from 'lucide-react';
import { compareAnalyses, downloadComparisonReport } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { AnalysisComparisonView } from '@/components/analysis-comparison-view';
import type { AnalysisComparison } from '@/lib/types';

function CompareContent() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const projectId = params.id as string;
  const baseline = searchParams.get('baseline');
  const current = searchParams.get('current');

  const [comparison, setComparison] = useState<AnalysisComparison | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState<'markdown' | 'json' | 'pdf' | null>(null);

  useEffect(() => {
    if (!baseline || !current) {
      setError('Selecione duas análises concluídas para comparar.');
      setLoading(false);
      return;
    }

    let cancelled = false;
    compareAnalyses(getTenantId(), projectId, baseline, current)
      .then((data) => {
        if (!cancelled) {
          setComparison(data);
          setError(null);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [projectId, baseline, current]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <button
        onClick={() => router.push(`/projects/${projectId}?tab=analyses`)}
        className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-6"
      >
        <ArrowLeft className="w-4 h-4" />
        Voltar ao projeto
      </button>

      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Comparativo before/after</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Evolução arquitetural entre duas análises concluídas — ideal para follow-up consultivo.
          </p>
        </div>
        {comparison && baseline && current && (
          <div className="flex flex-wrap gap-2">
            {(['markdown', 'json', 'pdf'] as const).map((format) => (
              <button
                key={format}
                type="button"
                disabled={!!exporting}
                onClick={async () => {
                  setExporting(format);
                  try {
                    await downloadComparisonReport(
                      getTenantId(),
                      projectId,
                      baseline,
                      current,
                      format
                    );
                  } finally {
                    setExporting(null);
                  }
                }}
                className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium rounded-lg border border-border hover:bg-muted transition-colors disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                {exporting === format
                  ? 'Exportando...'
                  : `Exportar ${format === 'markdown' ? 'Markdown' : format.toUpperCase()}`}
              </button>
            ))}
          </div>
        )}
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-xl p-4 text-sm">
          {error}
        </div>
      )}

      {comparison && <AnalysisComparisonView comparison={comparison} />}
    </div>
  );
}

export default function AnalysisComparePage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    }>
      <CompareContent />
    </Suspense>
  );
}
