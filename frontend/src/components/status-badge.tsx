'use client';

import clsx from 'clsx';
import { statusLabel } from '@/lib/labels';

const statusColors: Record<string, string> = {
  CREATED: 'bg-zinc-500/20 text-zinc-400',
  UPLOADING: 'bg-yellow-500/20 text-yellow-400',
  UPLOADED: 'bg-blue-500/20 text-blue-400',
  INGESTING: 'bg-indigo-500/20 text-indigo-400',
  READY: 'bg-green-500/20 text-green-400',
  FAILED: 'bg-red-500/20 text-red-400',
  PENDING: 'bg-yellow-500/20 text-yellow-400',
  PROCESSING: 'bg-indigo-500/20 text-indigo-400',
  COMPLETED: 'bg-green-500/20 text-green-400',
  PROPOSED: 'bg-blue-500/20 text-blue-400',
  ACCEPTED: 'bg-green-500/20 text-green-400',
  DEPRECATED: 'bg-zinc-500/20 text-zinc-400',
  SUPERSEDED: 'bg-orange-500/20 text-orange-400',
};

interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

export function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full font-medium',
        size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm',
        statusColors[status] || 'bg-zinc-500/20 text-zinc-400'
      )}
    >
      {statusLabel(status)}
    </span>
  );
}
