'use client';

import { useCallback, useEffect, useState } from 'react';
import { format } from 'date-fns';
import { Loader2, Mail, UserMinus } from 'lucide-react';
import { createOrgInvite, listOrgInvites, listOrgMembers, removeOrgMember } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { hasWriteAccess } from '@/lib/roles';
import type { OrgInvite, OrgMember, OrgMemberRole } from '@/lib/types';

const ROLE_LABELS: Record<OrgMemberRole, string> = {
  ORG_ADMIN: 'Administrador',
  ORG_MEMBER: 'Membro',
  ORG_VIEWER: 'Visualizador',
};

export default function TeamSettingsPage() {
  const [members, setMembers] = useState<OrgMember[]>([]);
  const [invites, setInvites] = useState<OrgInvite[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [canWrite, setCanWrite] = useState(false);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<OrgMemberRole>('ORG_MEMBER');
  const [inviting, setInviting] = useState(false);

  const loadData = useCallback(async () => {
    setError('');
    try {
      const tenantId = getTenantId();
      const [memberList, inviteList] = await Promise.all([
        listOrgMembers(tenantId),
        listOrgInvites(tenantId),
      ]);
      setMembers(memberList);
      setInvites(inviteList.filter((invite) => !invite.acceptedAt));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar equipe');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setCanWrite(hasWriteAccess());
    const timer = window.setTimeout(() => {
      void loadData();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadData]);

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim()) {
      return;
    }

    setInviting(true);
    setError('');
    try {
      await createOrgInvite(getTenantId(), email.trim(), role);
      setEmail('');
      setRole('ORG_MEMBER');
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao enviar convite');
    } finally {
      setInviting(false);
    }
  }

  async function handleRemoveMember(memberId: string) {
    if (!window.confirm('Remover este membro da organização?')) {
      return;
    }

    setError('');
    try {
      await removeOrgMember(getTenantId(), memberId);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao remover membro');
    }
  }

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-foreground">Equipe</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Gerencie membros e convites da organização
        </p>
      </div>

      {error && (
        <p className="mb-4 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3">
          {error}
        </p>
      )}

      {canWrite && (
        <form
          onSubmit={handleInvite}
          className="mb-8 bg-card rounded-xl border border-border p-6 space-y-4"
        >
          <h2 className="text-base font-semibold text-foreground">Convidar membro</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">E-mail</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="novo@empresa.com"
                required
                className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-foreground mb-1.5">Papel</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as OrgMemberRole)}
                className="w-full px-3 py-2 bg-background border border-border rounded-lg text-sm"
              >
                {(Object.keys(ROLE_LABELS) as OrgMemberRole[]).map((item) => (
                  <option key={item} value={item}>
                    {ROLE_LABELS[item]}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={inviting}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/80 disabled:opacity-50"
          >
            {inviting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Mail className="w-4 h-4" />}
            Enviar convite
          </button>
        </form>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary animate-spin" />
        </div>
      ) : (
        <div className="space-y-8">
          <section>
            <h2 className="text-base font-semibold text-foreground mb-4">Membros</h2>
            {members.length === 0 ? (
              <p className="text-sm text-muted-foreground">Nenhum membro cadastrado.</p>
            ) : (
              <div className="bg-card border border-border rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-secondary/30 text-muted-foreground">
                    <tr>
                      <th className="text-left px-4 py-3 font-medium">E-mail</th>
                      <th className="text-left px-4 py-3 font-medium">Papel</th>
                      <th className="text-left px-4 py-3 font-medium">Status</th>
                      <th className="text-left px-4 py-3 font-medium">Desde</th>
                      {canWrite && <th className="px-4 py-3" />}
                    </tr>
                  </thead>
                  <tbody>
                    {members.map((member) => (
                      <tr key={member.id} className="border-t border-border">
                        <td className="px-4 py-3 text-foreground">{member.email}</td>
                        <td className="px-4 py-3 text-muted-foreground">
                          {ROLE_LABELS[member.role] ?? member.role}
                        </td>
                        <td className="px-4 py-3 text-muted-foreground">{member.status}</td>
                        <td className="px-4 py-3 text-muted-foreground">
                          {format(new Date(member.createdAt), 'dd/MM/yyyy')}
                        </td>
                        {canWrite && (
                          <td className="px-4 py-3 text-right">
                            <button
                              type="button"
                              onClick={() => handleRemoveMember(member.id)}
                              className="inline-flex items-center gap-1 text-destructive hover:text-destructive/80 text-xs"
                            >
                              <UserMinus className="w-3.5 h-3.5" />
                              Remover
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section>
            <h2 className="text-base font-semibold text-foreground mb-4">Convites pendentes</h2>
            {invites.length === 0 ? (
              <p className="text-sm text-muted-foreground">Nenhum convite pendente.</p>
            ) : (
              <div className="bg-card border border-border rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-secondary/30 text-muted-foreground">
                    <tr>
                      <th className="text-left px-4 py-3 font-medium">E-mail</th>
                      <th className="text-left px-4 py-3 font-medium">Papel</th>
                      <th className="text-left px-4 py-3 font-medium">Expira em</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invites.map((invite) => (
                      <tr key={invite.id} className="border-t border-border">
                        <td className="px-4 py-3 text-foreground">{invite.email}</td>
                        <td className="px-4 py-3 text-muted-foreground">
                          {ROLE_LABELS[invite.role] ?? invite.role}
                        </td>
                        <td className="px-4 py-3 text-muted-foreground">
                          {format(new Date(invite.expiresAt), 'dd/MM/yyyy HH:mm')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
