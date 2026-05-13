'use client';

import { useCallback, useState } from 'react';
import { Upload } from 'lucide-react';
import clsx from 'clsx';

interface FileDropZoneProps {
  onFileSelected: (file: File) => void;
  accept?: string;
  loading?: boolean;
}

export function FileDropZone({ onFileSelected, accept = '.zip', loading = false }: FileDropZoneProps) {
  const [dragOver, setDragOver] = useState(false);
  const [fileName, setFileName] = useState<string | null>(null);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragOver(false);
      const file = e.dataTransfer.files[0];
      if (file) {
        setFileName(file.name);
        onFileSelected(file);
      }
    },
    [onFileSelected]
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) {
        setFileName(file.name);
        onFileSelected(file);
      }
    },
    [onFileSelected]
  );

  return (
    <label
      onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      className={clsx(
        'flex flex-col items-center justify-center gap-3 p-8 rounded-xl border-2 border-dashed cursor-pointer transition-colors',
        dragOver ? 'border-primary bg-primary/5' : 'border-border hover:border-muted-foreground',
        loading && 'opacity-50 pointer-events-none'
      )}
    >
      <Upload className={clsx('w-8 h-8', dragOver ? 'text-primary' : 'text-muted-foreground')} />
      {fileName ? (
        <span className="text-sm text-foreground font-medium">{fileName}</span>
      ) : (
        <div className="text-center">
          <p className="text-sm text-foreground">Drop your .zip file here or click to browse</p>
          <p className="text-xs text-muted-foreground mt-1">Only .zip files are supported</p>
        </div>
      )}
      {loading && <p className="text-xs text-primary animate-pulse">Uploading...</p>}
      <input type="file" accept={accept} onChange={handleChange} className="hidden" />
    </label>
  );
}
