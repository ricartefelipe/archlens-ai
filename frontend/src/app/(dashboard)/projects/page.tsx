'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, FolderOpen, Loader2, AlertCircle } from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { listProjects, createProject } from '@/lib/api';
import { parseApiError } from '@/lib/api-error';
import { getTenantId } from '@/lib/auth';
import { StatusBadge } from '@/components/status-badge';
import { OnboardingGuide } from '@/components/onboarding-guide';
import type { Project } from '@/lib/types';

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const loadProjects = useCallback(async () => {
    try {
      const data = await listProjects(getTenantId());
      setProjects(data);
    } catch (err) {
      setErrorMessage(parseApiError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const t = window.setTimeout(() => {
      void loadProjects();
    }, 0);
    return () => window.clearTimeout(t);
  }, [loadProjects]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setCreating(true);
    setErrorMessage(null);
    try {
      const project = await createProject(getTenantId(), name.trim(), description.trim());
      setProjects((prev) => [project, ...prev]);
      setName('');
      setDescription('');
      setShowForm(false);
    } catch (err) {
      setErrorMessage(parseApiError(err));
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Projetos</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Diagnósticos arquiteturais — upload, análise e relatório com evidências
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 px-4 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Novo projeto
        </button>
      </div>

      {errorMessage && (
        <div className="mb-6 flex items-start gap-2 rounded-xl border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
          <p>{errorMessage}</p>
        </div>
      )}

      {!loading && projects.length === 0 && !showForm && (
        <OnboardingGuide className="mb-8" />
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="mb-8 bg-card rounded-xl border border-border p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Nome</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="meu-sistema"
              required
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Descrição</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Opcional — contexto do sistema auditado"
              rows={2}
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring resize-none"
            />
          </div>
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={creating}
              className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50 transition-colors"
            >
              {creating && <Loader2 className="w-4 h-4 animate-spin" />}
              Criar
            </button>
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              Cancelar
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
        </div>
      ) : projects.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <FolderOpen className="w-12 h-12 text-muted-foreground mb-4" />
          <p className="text-lg text-foreground font-medium">Nenhum projeto ainda</p>
          <p className="text-sm text-muted-foreground mt-1">Crie um projeto e envie um ZIP para começar</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map((project) => (
            <button
              key={project.id}
              onClick={() => router.push(`/projects/${project.id}`)}
              className="bg-card border border-border rounded-xl p-5 text-left hover:border-primary/50 hover:shadow-lg hover:shadow-primary/5 transition-all group"
            >
              <div className="flex items-start justify-between mb-3">
                <h3 className="text-base font-semibold text-foreground group-hover:text-primary transition-colors truncate">
                  {project.name}
                </h3>
                <StatusBadge status={project.status} />
              </div>
              {project.description && (
                <p className="text-sm text-muted-foreground mb-3 line-clamp-2">{project.description}</p>
              )}
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>{project.fileCount} arquivo(s)</span>
                <span>{format(new Date(project.createdAt), 'd MMM yyyy', { locale: ptBR })}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
