'use client';

import clsx from 'clsx';
import { format } from 'date-fns';

interface ChatMessageProps {
  type: 'user' | 'assistant';
  content: string;
  sources?: string;
  timestamp: string;
  loading?: boolean;
}

interface SourceRef {
  file_path?: string;
  filePath?: string;
  chunk_index?: number;
  chunkIndex?: number;
  score?: number;
}

export function ChatMessage({ type, content, sources, timestamp, loading }: ChatMessageProps) {
  let parsedSources: SourceRef[] = [];
  if (sources) {
    try {
      parsedSources = JSON.parse(sources);
    } catch {
      // ignore parse errors
    }
  }

  return (
    <div className={clsx('flex', type === 'user' ? 'justify-end' : 'justify-start')}>
      <div
        className={clsx(
          'max-w-[80%] rounded-2xl px-4 py-3 space-y-2',
          type === 'user' ? 'bg-primary/20 text-foreground' : 'bg-card text-card-foreground'
        )}
      >
        {loading ? (
          <div className="flex gap-1 py-1">
            <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: '0ms' }} />
            <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: '150ms' }} />
            <span className="w-2 h-2 rounded-full bg-muted-foreground animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
        ) : (
          <div className="text-sm whitespace-pre-wrap break-words">{content}</div>
        )}

        {parsedSources.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-1">
            {parsedSources.map((src, i) => (
              <span
                key={i}
                className="inline-flex items-center px-2 py-0.5 rounded text-xs bg-secondary text-muted-foreground"
              >
                {src.file_path || src.filePath}
                {(src.chunk_index ?? src.chunkIndex) != null && ` #${src.chunk_index ?? src.chunkIndex}`}
              </span>
            ))}
          </div>
        )}

        <p className="text-xs text-muted-foreground">
          {format(new Date(timestamp), 'HH:mm')}
        </p>
      </div>
    </div>
  );
}
