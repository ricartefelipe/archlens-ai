'use client';

import { useCallback, useEffect, useState } from 'react';
import { format } from 'date-fns';
import { Key, Loader2, Plus, Trash2 } from 'lucide-react';
import { createApiKey, listApiKeys, revokeApiKey } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { hasWriteAccess } from '@/lib/roles';
import type { ApiKey } from '@/lib/types';

export default function ApiKeysSettingsPage() {
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [canWrite, setCanWrite] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [creating, setCreating] = useState(false);
  const [plainKey, setPlainKey] = useState('');

  const loadKeys = useCallback(async () => {
    setError('');
    try {
      setKeys(await listApiKeys(getTenantId()));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar chaves API');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setCanWrite(hasWriteAccess());
    const timer = window.setTimeout(() => {
      void loadKeys();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadKeys]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      return;
    }

    setCreating(true);
    setError('');
    try {
      const created = await createApiKey(getTenantId(), name.trim());
      setPlainKey(created.plainKey);
      setName('');
      setShowForm(false);
      await loadKeys();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao criar chave API');
    } finally {
      setCreating(false);
    }
  }

  async function handleRevoke(keyId: string) {
    if (!window.confirm('Revogar esta chave API? Integrações que a utilizam deixarão de funcionar.')) {
      return;
    }

    setError('');
    try {
      await revokeApiKey(getTenantId(), keyId);
      await loadKeys();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao revogar chave');
    }
  }

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Chaves API</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Integre sistemas externos com autenticação via header X-Api-Key
          </p>
        </div>
        {canWrite && (
          <button
            type="button"
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80"
          >
            <Plus className="w-4 h-4" />
            Nova chave
          </button>
        )}
      </div>

      {error && (
        <p className="mb-4 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3">
          {error}
        </p>
      )}

      {plainKey && (
        <div className="mb-6 bg-success/10 border border-success/30 rounded-xl p-4 space-y-2">
          <p className="text-sm font-medium text-foreground">Chave criada — copie agora, ela não será exibida novamente:</p>
          <code className="block text-xs bg-background border border-border rounded-lg px-3 py-2 break-all">
            {plainKey}
          </code>
          <button
            type="button"
            onClick={() => setPlainKey('')}
            className="text-xs text-muted-foreground hover:text-foreground"
          >
            Fechar
          </button>
        </div>
      )}

      {showForm && canWrite && (
        <form onSubmit={handleCreate} className="mb-8 bg-card rounded-xl border border-border p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-foreground mb-1.5">Nome</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Integração CI/CD"
              required
              className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
            />
          </div>
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={creating}
              className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50"
            >
              {creating && <Loader2 className="w-4 h-4 animate-spin" />}
              Criar chave
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
      ) : keys.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <Key className="w-12 h-12 text-muted-foreground mb-4" />
          <p className="text-lg text-foreground font-medium">Nenhuma chave API</p>
          <p className="text-sm text-muted-foreground mt-1">Crie uma chave para integrações automatizadas</p>
        </div>
      ) : (
        <div className="bg-card border border-border rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-secondary/30 text-muted-foreground">
              <tr>
                <th className="text-left px-4 py-3 font-medium">Nome</th>
                <th className="text-left px-4 py-3 font-medium">Prefixo</th>
                <th className="text-left px-4 py-3 font-medium">Escopos</th>
                <th className="text-left px-4 py-3 font-medium">Criada em</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                {canWrite && <th className="px-4 py-3" />}
              </tr>
            </thead>
            <tbody>
              {keys.map((key) => (
                <tr key={key.id} className="border-t border-border">
                  <td className="px-4 py-3 text-foreground">{key.name}</td>
                  <td className="px-4 py-3 font-mono text-muted-foreground">{key.keyPrefix}…</td>
                  <td className="px-4 py-3 text-muted-foreground">{key.scopes}</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {format(new Date(key.createdAt), 'dd/MM/yyyy')}
                  </td>
                  <td className="px-4 py-3">
                    {key.revokedAt ? (
                      <span className="text-destructive">Revogada</span>
                    ) : (
                      <span className="text-success">Ativa</span>
                    )}
                  </td>
                  {canWrite && (
                    <td className="px-4 py-3 text-right">
                      {!key.revokedAt && (
                        <button
                          type="button"
                          onClick={() => handleRevoke(key.id)}
                          className="inline-flex items-center gap-1 text-destructive hover:text-destructive/80 text-xs"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          Revogar
                        </button>
                      )}
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
