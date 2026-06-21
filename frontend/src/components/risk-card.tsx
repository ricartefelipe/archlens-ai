'use client';

import { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import clsx from 'clsx';
import type { ArchitecturalRisk } from '@/lib/types';
import { riskCategoryLabel, severityLabel } from '@/lib/labels';

const severityColors: Record<string, { bg: string; text: string }> = {
  CRITICAL: { bg: 'bg-red-500/20', text: 'text-red-400' },
  HIGH: { bg: 'bg-orange-500/20', text: 'text-orange-400' },
  MEDIUM: { bg: 'bg-yellow-500/20', text: 'text-yellow-400' },
  LOW: { bg: 'bg-green-500/20', text: 'text-green-400' },
};

interface RiskCardProps {
  risk: ArchitecturalRisk;
  defaultExpanded?: boolean;
}

export function RiskCard({ risk, defaultExpanded = false }: RiskCardProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const colors = severityColors[risk.severity] || severityColors.MEDIUM;

  return (
    <div className="border border-border rounded-lg overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-secondary/30 transition-colors"
      >
        {expanded ? (
          <ChevronDown className="w-4 h-4 text-muted-foreground shrink-0" />
        ) : (
          <ChevronRight className="w-4 h-4 text-muted-foreground shrink-0" />
        )}
        <span className={clsx('px-2 py-0.5 rounded text-xs font-medium', colors.bg, colors.text)}>
          {severityLabel(risk.severity)}
        </span>
        <span className="text-sm font-medium text-foreground flex-1 truncate">{risk.title}</span>
        <span className="text-xs px-2 py-0.5 rounded bg-secondary text-muted-foreground">
          {risk.categoryLabel ?? riskCategoryLabel(risk.category)}
        </span>
        <span className="text-xs text-muted-foreground truncate max-w-[200px]">{risk.filePath}</span>
      </button>

      {expanded && (
        <div className="px-4 pb-4 pt-1 border-t border-border space-y-3">
          <p className="text-sm text-foreground">{risk.description}</p>
          {risk.evidence && (
            <div className="bg-background rounded-lg p-3">
              <p className="text-xs text-muted-foreground mb-1">Evidência</p>
              <pre className="text-xs text-foreground font-mono whitespace-pre-wrap break-words">
                {risk.evidence}
              </pre>
            </div>
          )}
          {risk.suggestion && (
            <div className="bg-primary/5 rounded-lg p-3">
              <p className="text-xs text-primary mb-1">Recomendação</p>
              <p className="text-sm text-foreground">{risk.suggestion}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
