"use client";

import { useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { RequestStatus, RequestView, STEPS } from "@/components/DocumentRequestsPanel";
import { ErrorBox, PageHeader, inputCls, primaryBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";

const BADGE: Record<RequestStatus, string> = {
  FILED: "bg-slate-200 text-slate-700",
  IN_PROGRESS: "bg-blue-100 text-blue-700",
  READY_FOR_PICKUP: "bg-emerald-100 text-emerald-700",
};
const label = (s: RequestStatus) => STEPS.find((x) => x.key === s)?.label ?? s;

export default function AdminRequestsPage() {
  const [items, setItems] = useState<RequestView[]>([]);
  const [filter, setFilter] = useState("");
  const [drafts, setDrafts] = useState<Record<number, { status: RequestStatus; note: string }>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const list = await apiFetch<RequestView[]>(`/api/admin/requests${filter ? `?status=${filter}` : ""}`);
      setItems(list);
      setDrafts(Object.fromEntries(list.map((r) => [r.id, { status: r.status, note: r.adminNote ?? "" }])));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401)
        setError(e.status === 403 ? "Vous n'avez pas la permission de traiter les demandes." : e.message);
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  async function save(r: RequestView) {
    const d = drafts[r.id];
    setSavingId(r.id);
    setError(null);
    try {
      const updated = await apiFetch<RequestView>(`/api/admin/requests/${r.id}`, {
        method: "PATCH",
        body: JSON.stringify({ status: d.status, note: d.note.trim() || null }),
      });
      setItems((prev) => prev.map((x) => (x.id === r.id ? updated : x)));
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    } finally {
      setSavingId(null);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Demandes de documents"
        subtitle="Traitez les demandes des étudiants et des professeurs : chaque changement d'état les prévient par notification et par e-mail."
        action={
          <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${inputCls} mt-0 w-64`}>
            <option value="">Toutes</option>
            {STEPS.map((s) => (
              <option key={s.key} value={s.key}>{s.label}</option>
            ))}
          </select>
        }
      />
      <ErrorBox message={error} />
      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Demandeur</th>
              <th className="px-5 py-3">Document</th>
              <th className="px-5 py-3">Motif</th>
              <th className="px-5 py-3">Date</th>
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {!loading && items.length === 0 && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Aucune demande.</td></tr>}
            {items.map((r) => {
              const d = drafts[r.id];
              if (!d) return null;
              return (
                <tr key={r.id} className="align-top">
                  <td className="px-5 py-4">
                    <span className="font-medium text-slate-900">{r.requesterName}</span>
                    <span className="block text-xs text-slate-500">{r.requesterRole} · {r.requesterEmail}</span>
                  </td>
                  <td className="px-5 py-4 text-slate-700">
                    {r.type}
                    <span className="block text-xs text-slate-500">{r.copies} ex.</span>
                  </td>
                  <td className="max-w-xs px-5 py-4 text-slate-600">
                    {r.reason}
                    {r.details && <span className="block text-xs text-slate-500">{r.details}</span>}
                  </td>
                  <td className="px-5 py-4 text-slate-600">{formatDate(r.createdAt)}</td>
                  <td className="px-5 py-4">
                    <span className={`mb-2 inline-block rounded-full px-3 py-1 text-xs font-semibold ${BADGE[r.status]}`}>{label(r.status)}</span>
                    <select value={d.status} onChange={(e) => setDrafts({ ...drafts, [r.id]: { ...d, status: e.target.value as RequestStatus } })}
                      className={`${inputCls} mt-1 min-w-48`}>
                      {STEPS.map((s) => (
                        <option key={s.key} value={s.key}>{s.label}</option>
                      ))}
                    </select>
                    <input value={d.note} maxLength={1000} placeholder="Note pour le demandeur (facultatif)"
                      onChange={(e) => setDrafts({ ...drafts, [r.id]: { ...d, note: e.target.value } })}
                      className={`${inputCls} mt-2`} />
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button onClick={() => save(r)} disabled={savingId === r.id} className={primaryBtn}>Enregistrer</button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}