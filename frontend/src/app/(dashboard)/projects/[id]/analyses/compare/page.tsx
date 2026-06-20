'use client';

import { Suspense, useEffect, useState } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { compareAnalyses } from '@/lib/api';
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

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-foreground">Comparativo before/after</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Evolução arquitetural entre duas análises concluídas — ideal para follow-up consultivo.
        </p>
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
