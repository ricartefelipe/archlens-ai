'use client';

import { useCallback, useEffect, useState } from 'react';
import { format } from 'date-fns';
import { Loader2, Plus, Trash2, Webhook } from 'lucide-react';
import { createWebhook, deleteWebhook, listWebhooks } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { hasWriteAccess } from '@/lib/roles';
import type { TenantWebhook } from '@/lib/types';

const DEFAULT_EVENT = 'analysis.completed';

export default function WebhooksSettingsPage() {
  const [webhooks, setWebhooks] = useState<TenantWebhook[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [canWrite, setCanWrite] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [url, setUrl] = useState('');
  const [creating, setCreating] = useState(false);

  const loadWebhooks = useCallback(async () => {
    setError('');
    try {
      setWebhooks(await listWebhooks(getTenantId()));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar webhooks');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setCanWrite(hasWriteAccess());
    const timer = window.setTimeout(() => {
      void loadWebhooks();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadWebhooks]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) {
      return;
    }

    setCreating(true);
    setError('');
    try {
      await createWebhook(getTenantId(), url.trim(), [DEFAULT_EVENT]);
      setUrl('');
      setShowForm(false);
      await loadWebhooks();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao criar webhook');
    } finally {
      setCreating(false);
    }
  }

  async function handleDelete(webhookId: string) {
    if (!window.confirm('Excluir este webhook?')) {
      return;
    }

    setError('');
    try {
      await deleteWebhook(getTenantId(), webhookId);
      await loadWebhooks();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao excluir webhook');
    }
  }

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Webhooks</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Receba notificações HTTP quando uma análise for concluída
          </p>
        </div>
        {canWrite && (
          <button
            type="button"
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80"
          >
            <Plus className="w-4 h-4" />
            Novo webhook
          </button>
        )}
      </div>

      {error && (
        <p className="mb-4 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3">
          {error}
        </p>
      )}

      {showForm && canWrite && (
        <form onSubmit={handleCreate} className="mb-8 bg-card rounded-xl border border-border p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">URL de destino</label>
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://api.suaempresa.com/webhooks/archlens"
              required
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Evento</label>
            <input
              type="text"
              value={DEFAULT_EVENT}
              readOnly
              className="w-full px-3 py-2 bg-secondary/30 border border-border rounded-lg text-sm text-muted-foreground"
            />
          </div>
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={creating}
              className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50"
            >
              {creating && <Loader2 className="w-4 h-4 animate-spin" />}
              Criar webhook
            </button>
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm text-muted-foreground hover:text-foreground"
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
      ) : webhooks.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <Webhook className="w-12 h-12 text-muted-foreground mb-4" />
          <p className="text-lg text-foreground font-medium">Nenhum webhook configurado</p>
          <p className="text-sm text-muted-foreground mt-1">
            Evento suportado: {DEFAULT_EVENT}
          </p>
        </div>
      ) : (
        <div className="bg-card border border-border rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-secondary/30 text-muted-foreground">
              <tr>
                <th className="text-left px-4 py-3 font-medium">URL</th>
                <th className="text-left px-4 py-3 font-medium">Eventos</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                <th className="text-left px-4 py-3 font-medium">Criado em</th>
                {canWrite && <th className="px-4 py-3" />}
              </tr>
            </thead>
            <tbody>
              {webhooks.map((hook) => (
                <tr key={hook.id} className="border-t border-border">
                  <td className="px-4 py-3 text-foreground break-all">{hook.url}</td>
                  <td className="px-4 py-3 text-muted-foreground">{hook.events}</td>
                  <td className="px-4 py-3">
                    {hook.enabled ? (
                      <span className="text-success">Ativo</span>
                    ) : (
                      <span className="text-muted-foreground">Inativo</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {format(new Date(hook.createdAt), 'dd/MM/yyyy')}
                  </td>
                  {canWrite && (
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => handleDelete(hook.id)}
                        className="inline-flex items-center gap-1 text-destructive hover:text-destructive/80 text-xs"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Excluir
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
