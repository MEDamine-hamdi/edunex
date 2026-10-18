"use client";

import { useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, PageHeader, inputCls, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { roleLabels } from "@/lib/roles";

type Permission = { id: number; code: string; description: string | null; roles: string[] };

export default function PermissionsPage() {
  const [items, setItems] = useState<Permission[]>([]);
  const [drafts, setDrafts] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const list = await apiFetch<Permission[]>("/api/platform/permissions");
      setItems(list);
      setDrafts(Object.fromEntries(list.map((p) => [p.id, p.description ?? ""])));
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

  async function save(p: Permission) {
    setError(null);
    setInfo(null);
    try {
      const updated = await apiFetch<Permission>(`/api/platform/permissions/${p.id}`, {
        method: "PUT",
        body: JSON.stringify({ description: drafts[p.id] ?? "" }),
      });
      setItems((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
      setInfo(`Description de ${p.code} enregistrée.`);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <PlatformShell>
      <PageHeader
        title="Permissions"
        subtitle="Les permissions appliquées par le backend. Leur attribution aux rôles se fait dans la page Rôles."
      />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Code</th>
              <th className="px-5 py-3">Description</th>
              <th className="px-5 py-3">Détenue par</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={4} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {items.map((p) => (
              <tr key={p.id}>
                <td className="px-5 py-3 font-mono text-xs text-slate-900">{p.code}</td>
                <td className="px-5 py-3">
                  <input
                    value={drafts[p.id] ?? ""}
                    maxLength={255}
                    onChange={(e) => setDrafts((d) => ({ ...d, [p.id]: e.target.value }))}
                    className={`${inputCls} mt-0 min-w-72`}
                  />
                </td>
                <td className="px-5 py-3">
                  <div className="flex flex-wrap gap-1.5">
                    {p.roles.length === 0 && <span className="text-slate-400">Aucun rôle</span>}
                    {p.roles.map((r) => (
                      <span key={r} className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700">
                        {roleLabels[r] ?? r}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="px-5 py-3 text-right">
                  <button
                    onClick={() => save(p)}
                    disabled={(drafts[p.id] ?? "") === (p.description ?? "")}
                    className={smallBtn}
                  >
                    Enregistrer
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </PlatformShell>
  );
}