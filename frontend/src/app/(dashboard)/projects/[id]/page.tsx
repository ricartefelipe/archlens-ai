'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Loader2, Play, MessageSquare, FileCode, BarChart3 } from 'lucide-react';
import { format } from 'date-fns';
import clsx from 'clsx';
import {
  getProject, listProjectFiles, listAnalyses,
  uploadProjectZip, createAnalysis, getAnalysis,
} from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { StatusBadge } from '@/components/status-badge';
import { FileDropZone } from '@/components/file-drop-zone';
import type { Project, ProjectFile, Analysis } from '@/lib/types';

type Tab = 'upload' | 'files' | 'analyses';

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [project, setProject] = useState<Project | null>(null);
  const [files, setFiles] = useState<ProjectFile[]>([]);
  const [analyses, setAnalyses] = useState<Analysis[]>([]);
  const [tab, setTab] = useState<Tab>('upload');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [creatingAnalysis, setCreatingAnalysis] = useState(false);
  const [pollingIds, setPollingIds] = useState<Set<string>>(new Set());

  const loadData = useCallback(async () => {
    const tenantId = getTenantId();
    try {
      const [proj, fileList, analysisList] = await Promise.all([
        getProject(tenantId, projectId),
        listProjectFiles(tenantId, projectId),
        listAnalyses(tenantId, projectId),
      ]);
      setProject(proj);
      setFiles(fileList);
      setAnalyses(analysisList);
      if (fileList.length > 0) setTab('files');
    } catch (err) {
      console.error('Failed to load project data:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    if (pollingIds.size === 0) return;

    const interval = setInterval(async () => {
      const tenantId = getTenantId();
      const updates = await Promise.all(
        Array.from(pollingIds).map((id) => getAnalysis(tenantId, projectId, id).catch(() => null))
      );

      const completed = new Set<string>();
      setAnalyses((prev) =>
        prev.map((a) => {
          const update = updates.find((u) => u?.id === a.id);
          if (update && (update.status === 'COMPLETED' || update.status === 'FAILED')) {
            completed.add(update.id);
          }
          return update || a;
        })
      );

      if (completed.size > 0) {
        setPollingIds((prev) => {
          const next = new Set(prev);
          completed.forEach((id) => next.delete(id));
          return next;
        });
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [pollingIds, projectId]);

  async function handleUpload(file: File) {
    setUploading(true);
    try {
      await uploadProjectZip(getTenantId(), projectId, file);
      await loadData();
      setTab('files');
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      setUploading(false);
    }
  }

  async function handleRunAnalysis() {
    setCreatingAnalysis(true);
    try {
      const analysis = await createAnalysis(getTenantId(), projectId);
      setAnalyses((prev) => [analysis, ...prev]);
      setPollingIds((prev) => new Set(prev).add(analysis.id));
      setTab('analyses');
    } catch (err) {
      console.error('Failed to create analysis:', err);
    } finally {
      setCreatingAnalysis(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    );
  }

  if (!project) {
    return <div className="p-8 text-destructive">Project not found</div>;
  }

  const tabs: { key: Tab; label: string; icon: typeof FileCode }[] = [
    { key: 'upload', label: 'Upload', icon: FileCode },
    { key: 'files', label: `Files (${files.length})`, icon: FileCode },
    { key: 'analyses', label: `Analyses (${analyses.length})`, icon: BarChart3 },
  ];

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex items-start justify-between mb-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-2xl font-bold text-foreground">{project.name}</h1>
            <StatusBadge status={project.status} size="md" />
          </div>
          {project.description && (
            <p className="text-sm text-muted-foreground">{project.description}</p>
          )}
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => {
              const completed = analyses.find((a) => a.status === 'COMPLETED');
              const sorted = [...analyses].sort(
                (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
              );
              const target = completed ?? sorted[0];
              const href = target
                ? `/projects/${projectId}/chat?analysisId=${target.id}`
                : `/projects/${projectId}/chat`;
              router.push(href);
            }}
            className="flex items-center gap-2 px-4 py-2 border border-border text-foreground rounded-lg text-sm font-medium hover:bg-secondary/50 transition-colors"
          >
            <MessageSquare className="w-4 h-4" />
            Chat
          </button>
          <button
            onClick={handleRunAnalysis}
            disabled={creatingAnalysis}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50 transition-colors"
          >
            {creatingAnalysis ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
            Run Analysis
          </button>
        </div>
      </div>

      <div className="flex gap-1 mb-6 border-b border-border">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={clsx(
              'flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors',
              tab === t.key
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            <t.icon className="w-4 h-4" />
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'upload' && (
        <div className="max-w-xl">
          <FileDropZone onFileSelected={handleUpload} loading={uploading} />
        </div>
      )}

      {tab === 'files' && (
        <div className="bg-card rounded-xl border border-border overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Path</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Type</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-muted-foreground uppercase">Size</th>
              </tr>
            </thead>
            <tbody>
              {files.map((f) => (
                <tr key={f.id} className="border-b border-border/50 hover:bg-secondary/20">
                  <td className="px-4 py-2.5 text-foreground font-mono text-xs">{f.filePath}</td>
                  <td className="px-4 py-2.5">
                    <span className="px-2 py-0.5 rounded text-xs bg-secondary text-muted-foreground">{f.fileType}</span>
                  </td>
                  <td className="px-4 py-2.5 text-right text-muted-foreground">{formatBytes(f.sizeBytes)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'analyses' && (
        <div className="space-y-3">
          {analyses.length === 0 ? (
            <p className="text-sm text-muted-foreground py-8 text-center">No analyses yet. Click &quot;Run Analysis&quot; to start.</p>
          ) : (
            analyses.map((a) => (
              <button
                key={a.id}
                onClick={() => router.push(`/projects/${projectId}/analyses/${a.id}`)}
                className="w-full bg-card border border-border rounded-xl p-4 text-left hover:border-primary/50 transition-all"
              >
                <div className="flex items-center justify-between mb-2">
                  <StatusBadge status={a.status} />
                  <span className="text-xs text-muted-foreground">
                    {format(new Date(a.createdAt), 'MMM d, yyyy HH:mm')}
                  </span>
                </div>
                {a.summary && (
                  <p className="text-sm text-muted-foreground line-clamp-2">{a.summary}</p>
                )}
                {a.risks.length > 0 && (
                  <p className="text-xs text-muted-foreground mt-2">{a.risks.length} risks identified</p>
                )}
                {(a.status === 'PENDING' || a.status === 'PROCESSING') && (
                  <div className="flex items-center gap-2 mt-2 text-xs text-primary">
                    <Loader2 className="w-3 h-3 animate-spin" />
                    Processing...
                  </div>
                )}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
