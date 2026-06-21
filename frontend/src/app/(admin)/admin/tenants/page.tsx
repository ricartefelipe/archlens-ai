'use client';

import { useCallback, useEffect, useState } from 'react';
import { format } from 'date-fns';
import { Loader2, Shield } from 'lucide-react';
import {
  listAdminTenants,
  updateAdminTenantStatus,
  upgradeAdminTenantPlan,
} from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import type { AdminTenant } from '@/lib/types';

const PLANS = [
  { value: 'PILOT', label: 'Piloto' },
  { value: 'DIAGNOSTICO', label: 'Diagnóstico' },
  { value: 'PORTFOLIO', label: 'Portfólio' },
  { value: 'INTERNO', label: 'Interno' },
];

export default function AdminTenantsPage() {
  const [tenants, setTenants] = useState<AdminTenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedTenant, setSelectedTenant] = useState<string | null>(null);
  const [plan, setPlan] = useState('PILOT');
  const [notes, setNotes] = useState('');
  const [upgrading, setUpgrading] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState<string | null>(null);

  const loadTenants = useCallback(async () => {
    setError('');
    try {
      setTenants(await listAdminTenants(getTenantId()));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar tenants');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadTenants();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadTenants]);

  async function handleUpgrade(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant) {
      return;
    }

    setUpgrading(true);
    setError('');
    try {
      await upgradeAdminTenantPlan(getTenantId(), selectedTenant, plan, notes.trim());
      setNotes('');
      await loadTenants();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao atualizar plano');
    } finally {
      setUpgrading(false);
    }
  }

  async function handleStatusChange(targetTenantId: string, status: 'ACTIVE' | 'SUSPENDED') {
    setStatusUpdating(targetTenantId);
    setError('');
    try {
      await updateAdminTenantStatus(getTenantId(), targetTenantId, status);
      await loadTenants();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao atualizar status');
    } finally {
      setStatusUpdating(null);
    }
  }

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
          <Shield className="w-7 h-7 text-primary" />
          Administração de tenants
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Gerencie planos comerciais e status das contas
        </p>
      </div>

      {error && (
        <p className="mb-4 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3">
          {error}
        </p>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
        </div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
          <div className="xl:col-span-2 bg-card border border-border rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-secondary/30 text-muted-foreground">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Tenant</th>
                  <th className="text-left px-4 py-3 font-medium">Plano</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-left px-4 py-3 font-medium">Uso</th>
                  <th className="text-left px-4 py-3 font-medium">Período</th>
                  <th className="px-4 py-3 font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {tenants.map((tenant) => (
                  <tr
                    key={tenant.tenantId}
                    className={`border-t border-border ${selectedTenant === tenant.tenantId ? 'bg-primary/5' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => setSelectedTenant(tenant.tenantId)}
                        className="text-left text-foreground hover:text-primary font-medium"
                      >
                        {tenant.tenantId}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {tenant.planDisplayName || tenant.plan}
                    </td>
                    <td className="px-4 py-3">
                      {tenant.status === 'ACTIVE' ? (
                        <span className="text-success">Ativo</span>
                      ) : (
                        <span className="text-destructive">Suspenso</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">
                      {tenant.projectsUsed}/{tenant.projectsLimit} proj ·{' '}
                      {tenant.analysesUsed}/{tenant.analysesLimit} análises
                    </td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">
                      {format(new Date(tenant.usagePeriodStart), 'dd/MM/yyyy')}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-1">
                        {tenant.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            disabled={statusUpdating === tenant.tenantId}
                            onClick={() => handleStatusChange(tenant.tenantId, 'SUSPENDED')}
                            className="text-xs text-destructive hover:text-destructive/80 disabled:opacity-50"
                          >
                            Suspender
                          </button>
                        ) : (
                          <button
                            type="button"
                            disabled={statusUpdating === tenant.tenantId}
                            onClick={() => handleStatusChange(tenant.tenantId, 'ACTIVE')}
                            className="text-xs text-success hover:text-success/80 disabled:opacity-50"
                          >
                            Ativar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <form
            onSubmit={handleUpgrade}
            className="bg-card border border-border rounded-xl p-6 space-y-4 h-fit"
          >
            <h2 className="text-base font-semibold text-foreground">Upgrade de plano</h2>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Tenant</label>
              <select
                value={selectedTenant ?? ''}
                onChange={(e) => setSelectedTenant(e.target.value || null)}
                required
                className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
              >
                <option value="">Selecione…</option>
                {tenants.map((tenant) => (
                  <option key={tenant.tenantId} value={tenant.tenantId}>
                    {tenant.tenantId}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Plano</label>
              <select
                value={plan}
                onChange={(e) => setPlan(e.target.value)}
                className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
              >
                {PLANS.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Observações</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                placeholder="Contrato, nota interna…"
                className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm resize-none"
              />
            </div>
            <button
              type="submit"
              disabled={upgrading || !selectedTenant}
              className="w-full flex items-center justify-center gap-2 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50"
            >
              {upgrading && <Loader2 className="w-4 h-4 animate-spin" />}
              Aplicar upgrade
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
