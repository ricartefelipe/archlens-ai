'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { Loader2, Play, MessageSquare, FileCode, BarChart3, GitCompare, AlertCircle } from 'lucide-react';
import { format } from 'date-fns';
import clsx from 'clsx';
import {
  getProject, listProjectFiles, listAnalyses,
  uploadProjectZip, createAnalysis, getAnalysis,
} from '@/lib/api';
import { parseApiError } from '@/lib/api-error';
import { getTenantId } from '@/lib/auth';
import { analysisBlockReason, isProjectBusy, projectBusyMessage } from '@/lib/project-status';
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
  const searchParams = useSearchParams();
  const projectId = params.id as string;

  const [project, setProject] = useState<Project | null>(null);
  const [files, setFiles] = useState<ProjectFile[]>([]);
  const [analyses, setAnalyses] = useState<Analysis[]>([]);
  const [creatingAnalysis, setCreatingAnalysis] = useState(false);
  const [pollingIds, setPollingIds] = useState<Set<string>>(new Set());
  const [selectedForCompare, setSelectedForCompare] = useState<string[]>([]);
  const initialTab = searchParams.get('tab');
  const [tab, setTab] = useState<Tab>(
    initialTab === 'files' || initialTab === 'analyses' || initialTab === 'upload'
      ? initialTab
      : 'upload'
  );
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [lastUploadName, setLastUploadName] = useState<string | null>(null);
  const didAutoSelectTab = useRef(false);

  const loadData = useCallback(async (options?: { preserveTab?: boolean }) => {
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
      if (
        !options?.preserveTab
        && !didAutoSelectTab.current
        && fileList.length > 0
        && !initialTab
      ) {
        setTab('files');
        didAutoSelectTab.current = true;
      }
    } catch (err) {
      console.error('Failed to load project data:', err);
      setErrorMessage(parseApiError(err));
    } finally {
      setLoading(false);
    }
  }, [projectId, initialTab]);

  useEffect(() => {
    const t = window.setTimeout(() => {
      void loadData();
    }, 0);
    return () => window.clearTimeout(t);
  }, [loadData]);

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

  useEffect(() => {
    if (!project || !isProjectBusy(project)) {
      return;
    }

    const interval = setInterval(() => {
      void loadData({ preserveTab: true });
    }, 3000);

    return () => clearInterval(interval);
  }, [project?.status, project?.id, loadData]);

  async function handleUpload(file: File) {
    setUploading(true);
    setErrorMessage(null);
    setLastUploadName(file.name);
    try {
      await uploadProjectZip(getTenantId(), projectId, file);
      await loadData({ preserveTab: true });
      setTab('files');
    } catch (err) {
      console.error('Upload failed:', err);
      setErrorMessage(parseApiError(err));
    } finally {
      setUploading(false);
    }
  }

  async function handleRunAnalysis() {
    if (!project) {
      return;
    }

    const blockReason = analysisBlockReason(project);
    if (blockReason) {
      setErrorMessage(blockReason);
      return;
    }

    setCreatingAnalysis(true);
    setErrorMessage(null);
    try {
      const analysis = await createAnalysis(getTenantId(), projectId);
      setAnalyses((prev) => [analysis, ...prev]);
      setPollingIds((prev) => new Set(prev).add(analysis.id));
      setTab('analyses');
    } catch (err) {
      console.error('Failed to create analysis:', err);
      setErrorMessage(parseApiError(err));
    } finally {
      setCreatingAnalysis(false);
    }
  }

  function toggleCompareSelection(analysisId: string) {
    setSelectedForCompare((prev) => {
      if (prev.includes(analysisId)) {
        return prev.filter((id) => id !== analysisId);
      }
      if (prev.length >= 2) {
        return [prev[1], analysisId];
      }
      return [...prev, analysisId];
    });
  }

  function handleCompare() {
    if (selectedForCompare.length !== 2) {
      return;
    }
    const sorted = [...selectedForCompare].sort((a, b) => {
      const aDate = analyses.find((item) => item.id === a)?.createdAt ?? '';
      const bDate = analyses.find((item) => item.id === b)?.createdAt ?? '';
      return new Date(aDate).getTime() - new Date(bDate).getTime();
    });
    router.push(
      `/projects/${projectId}/analyses/compare?baseline=${sorted[0]}&current=${sorted[1]}`
    );
  }

  const completedAnalyses = analyses.filter((a) => a.status === 'COMPLETED');
  const analysisBlocked = project ? analysisBlockReason(project) : 'Carregando projeto…';
  const busyMessage = project ? projectBusyMessage(project) : null;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    );
  }

  if (!project) {
    return <div className="p-8 text-destructive">Projeto não encontrado</div>;
  }

  const tabs: { key: Tab; label: string; icon: typeof FileCode }[] = [
    { key: 'upload', label: 'Upload', icon: FileCode },
    { key: 'files', label: `Arquivos (${files.length})`, icon: FileCode },
    { key: 'analyses', label: `Análises (${analyses.length})`, icon: BarChart3 },
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
            disabled={creatingAnalysis || !!analysisBlocked}
            title={analysisBlocked ?? undefined}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50 transition-colors"
          >
            {creatingAnalysis ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
            Executar análise
          </button>
        </div>
      </div>

      {(errorMessage || uploading || busyMessage) && (
        <div className="mb-6 space-y-2">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
              <p>{errorMessage}</p>
            </div>
          )}
          {(uploading || busyMessage) && (
            <div className="flex items-center gap-2 rounded-xl border border-primary/30 bg-primary/5 px-4 py-3 text-sm text-foreground">
              <Loader2 className="w-4 h-4 animate-spin text-primary shrink-0" />
              <p>
                {uploading
                  ? `Enviando ${lastUploadName ?? 'ZIP'}…`
                  : busyMessage}
              </p>
            </div>
          )}
        </div>
      )}

      {analysisBlocked && !errorMessage && (
        <p className="mb-4 text-xs text-muted-foreground">{analysisBlocked}</p>
      )}

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
          {files.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-muted-foreground">
              {project.status === 'UPLOADING'
                ? 'Substituindo arquivos do upload em andamento…'
                : 'Nenhum arquivo indexado. Use a aba Upload.'}
            </p>
          ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Caminho</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Tipo</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-muted-foreground uppercase">Tamanho</th>
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
          )}
        </div>
      )}

      {tab === 'analyses' && (
        <div className="space-y-3">
          {completedAnalyses.length >= 2 && (
            <div className="flex items-center justify-between bg-card border border-border rounded-xl p-4">
              <div>
                <p className="text-sm font-medium text-foreground">Comparar evolução</p>
                <p className="text-xs text-muted-foreground">
                  Selecione 2 análises concluídas ({selectedForCompare.length}/2)
                </p>
              </div>
              <button
                onClick={handleCompare}
                disabled={selectedForCompare.length !== 2}
                className="flex items-center gap-2 px-4 py-2 bg-secondary text-foreground rounded-lg text-sm font-medium hover:bg-secondary/80 disabled:opacity-50 transition-colors"
              >
                <GitCompare className="w-4 h-4" />
                Comparar antes/depois
              </button>
            </div>
          )}
          {analyses.length === 0 ? (
            <p className="text-sm text-muted-foreground py-8 text-center">
              Nenhuma análise ainda. Clique em &quot;Executar análise&quot; para começar.
            </p>
          ) : (
            analyses.map((a) => (
              <div
                key={a.id}
                className="w-full bg-card border border-border rounded-xl p-4 hover:border-primary/50 transition-all"
              >
                <div className="flex items-start gap-3">
                  {a.status === 'COMPLETED' && completedAnalyses.length >= 2 && (
                    <input
                      type="checkbox"
                      checked={selectedForCompare.includes(a.id)}
                      onChange={() => toggleCompareSelection(a.id)}
                      className="mt-1 h-4 w-4 rounded border-border"
                      aria-label={`Selecionar análise ${a.id} para comparar`}
                    />
                  )}
                  <button
                    onClick={() => router.push(`/projects/${projectId}/analyses/${a.id}`)}
                    className="flex-1 text-left"
                  >
                <div className="flex items-center justify-between mb-2">
                  <StatusBadge status={a.status} />
                  <span className="text-xs text-muted-foreground">
                    {format(new Date(a.createdAt), 'dd/MM/yyyy HH:mm')}
                  </span>
                </div>
                {a.summary && (
                  <p className="text-sm text-muted-foreground line-clamp-2">{a.summary}</p>
                )}
                {a.risks.length > 0 && (
                  <p className="text-xs text-muted-foreground mt-2">{a.risks.length} risco(s) identificado(s)</p>
                )}
                {(a.status === 'PENDING' || a.status === 'PROCESSING') && (
                  <div className="flex items-center gap-2 mt-2 text-xs text-primary">
                    <Loader2 className="w-3 h-3 animate-spin" />
                    Processando…
                  </div>
                )}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
