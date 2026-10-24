"use client";

import { SectionLoader } from "@/components/Loading";
import { useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, IdPicker, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { roleLabels } from "@/lib/roles";

type PermissionRef = { id: number; code: string; description: string | null };
type Role = {
  id: number;
  name: string;
  description: string | null;
  locked: boolean;
  userCount: number;
  permissions: PermissionRef[];
};
type Permission = { id: number; code: string; description: string | null };

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [r, p] = await Promise.all([
        apiFetch<Role[]>("/api/platform/roles"),
        apiFetch<Permission[]>("/api/platform/permissions"),
      ]);
      setRoles(r);
      setPermissions(p);
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function save(role: Role, ids: number[]) {
    setSavingId(role.id);
    setError(null);
    setInfo(null);
    try {
      const updated = await apiFetch<Role>(`/api/platform/roles/${role.id}/permissions`, {
        method: "PUT",
        body: JSON.stringify({ ids }),
      });
      setRoles((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
      setInfo(`Permissions de « ${roleLabels[role.name] ?? role.name} » enregistrées. Elles s'appliquent dès la prochaine requête.`);
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 409 ? "Ce rôle est verrouillé." : err.message);
    } finally {
      setSavingId(null);
    }
  }

  return (
    <PlatformShell>
      <PageHeader
        title="Rôles"
        subtitle="Choisissez les permissions de chaque rôle. Les quatre rôles sont fixes : ils déterminent les interfaces."
      />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}
      <p className="mt-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600">
        Une permission n&apos;a d&apos;effet que combinée au rôle prévu pour l&apos;action. Certaines pages utilisent
        plusieurs permissions : la page Notes lit aussi la liste des matières (STRUCTURE_MANAGE), la page
        Inscriptions lit les étudiants (ACCOUNTS_MANAGE).
      </p>

      {loading && <SectionLoader />}
      <div className="mt-6 space-y-6">
        {roles.map((r) => (
          <section key={r.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">{roleLabels[r.name] ?? r.name}</h2>
                <p className="font-mono text-xs text-slate-500">{r.name}</p>
              </div>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
                {r.userCount} utilisateur{r.userCount > 1 ? "s" : ""}
              </span>
            </div>
            <div className="mt-4">
              {r.locked ? (
                <p className="text-sm text-slate-500">
                  Ce rôle est verrouillé : l&apos;administrateur de la plateforme conserve un accès complet.
                </p>
              ) : (
                <IdPicker
                  options={permissions.map((p) => ({ id: p.id, label: `${p.code}${p.description ? ` : ${p.description}` : ""}` }))}
                  selected={r.permissions.map((p) => p.id)}
                  onSave={(ids) => save(r, ids)}
                  saving={savingId === r.id}
                  emptyText="Aucune permission définie."
                />
              )}
            </div>
          </section>
        ))}
      </div>
    </PlatformShell>
  );
}