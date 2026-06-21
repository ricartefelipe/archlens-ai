'use client';

import { Upload, Play, FileText, MessageSquare } from 'lucide-react';
import clsx from 'clsx';

const STEPS = [
  {
    icon: Upload,
    title: '1. Envie o ZIP',
    description: 'Código, OpenAPI, Docker, CI e migrations do sistema auditado.',
  },
  {
    icon: Play,
    title: '2. Execute a análise',
    description: 'Achados estáticos com evidência rastreável por arquivo.',
  },
  {
    icon: FileText,
    title: '3. Exporte o relatório',
    description: 'Markdown, JSON ou PDF com sumário executivo para o cliente.',
  },
  {
    icon: MessageSquare,
    title: '4. Chat arquitetural',
    description: 'Perguntas sobre riscos com trechos do código como contexto.',
  },
] as const;

interface OnboardingGuideProps {
  compact?: boolean;
  className?: string;
}

export function OnboardingGuide({ compact = false, className }: OnboardingGuideProps) {
  return (
    <div
      className={clsx(
        'rounded-xl border border-primary/20 bg-primary/5',
        compact ? 'p-4' : 'p-6',
        className
      )}
    >
      <p className={clsx('font-medium text-foreground', compact ? 'text-sm mb-3' : 'text-base mb-4')}>
        Como funciona o diagnóstico
      </p>
      <div
        className={clsx(
          'grid gap-3',
          compact ? 'grid-cols-1 sm:grid-cols-2' : 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4'
        )}
      >
        {STEPS.map((step) => (
          <div key={step.title} className="flex gap-3 items-start">
            <div className="rounded-lg bg-primary/10 p-2 shrink-0">
              <step.icon className="w-4 h-4 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">{step.title}</p>
              <p className="text-xs text-muted-foreground mt-0.5">{step.description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
