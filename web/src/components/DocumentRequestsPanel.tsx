"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn } from "./ui";

export type RequestStatus = "FILED" | "IN_PROGRESS" | "READY_FOR_PICKUP";
export type RequestView = {
  id: number;
  type: string;
  copies: number;
  reason: string;
  details: string | null;
  status: RequestStatus;
  adminNote: string | null;
  handledBy: string | null;
  requesterName: string;
  requesterRole: string;
  requesterEmail: string;
  createdAt: string;
  updatedAt: string;
};

export const STEPS: { key: RequestStatus; label: string }[] = [
  { key: "FILED", label: "Déposée" },
  { key: "IN_PROGRESS", label: "En cours de traitement" },
  { key: "READY_FOR_PICKUP", label: "Prête pour retrait" },
];

function Steps({ status }: { status: RequestStatus }) {
  const current = STEPS.findIndex((s) => s.key === status);
  return (
    <ol className="mt-4 flex items-start">
      {STEPS.map((s, i) => (
        <li key={s.key} className="flex flex-1 items-start last:flex-none">
          <div className="flex w-28 flex-col items-center gap-1 text-center">
            <span
              className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                i <= current ? (current === 2 ? "bg-emerald-500 text-white" : "bg-blue-600 text-white") : "bg-slate-200 text-slate-500"
              }`}
            >
              {i < current || (i === current && current === 2) ? "✓" : i + 1}
            </span>
            <span className={`text-[11px] ${i === current ? "font-semibold text-slate-900" : "text-slate-500"}`}>{s.label}</span>
          </div>
          {i < STEPS.length - 1 && <span className={`mt-3.5 h-0.5 flex-1 ${i < current ? "bg-blue-600" : "bg-slate-200"}`} />}
        </li>
      ))}
    </ol>
  );
}

export function DocumentRequestsPanel() {
  const [items, setItems] = useState<RequestView[]>([]);
  const [types, setTypes] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ type: "", copies: "1", reason: "", details: "" });
  const other = form.type === "Autre document";

  const load = useCallback(async () => {
    try {
      const [mine, t] = await Promise.all([
        apiFetch<RequestView[]>("/api/requests/mine"),
        apiFetch<string[]>("/api/requests/types"),
      ]);
      setItems(mine);
      setTypes(t);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setInfo(null);
    try {
      const created = await apiFetch<RequestView>("/api/requests", {
        method: "POST",
        body: JSON.stringify({
          type: form.type,
          copies: Number(form.copies),
          reason: form.reason.trim(),
          details: form.details.trim() || null,
        }),
      });
      setItems((prev) => [created, ...prev]);
      setForm({ type: "", copies: "1", reason: "", details: "" });
      setShowForm(false);
      setInfo("Votre demande a été déposée. Vous serez prévenu(e) à chaque étape.");
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 400 ? "Vérifiez le formulaire : le motif doit contenir au moins 5 caractères." : err.message);
    } finally {
      setSaving(false);
    }
  }

  async function withdraw(r: RequestView) {
    if (!window.confirm(`Retirer la demande « ${r.type} » ?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/requests/${r.id}`, { method: "DELETE" });
      setItems((prev) => prev.filter((x) => x.id !== r.id));
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 409 ? "Cette demande est déjà en cours de traitement : elle ne peut plus être retirée." : err.message);
      load();
    }
  }

  return (
    <>
      <PageHeader
        title="Demandes de documents"
        subtitle="Demandez un document administratif et suivez son avancement."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouvelle demande"}
          </button>
        }
      />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {showForm && (
        <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="block text-sm font-medium text-slate-700 sm:col-span-2">
              Type de document
              <select required value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} className={inputCls}>
                <option value="">Choisir...</option>
                {types.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Nombre d&apos;exemplaires
              <input type="number" min={1} max={5} required value={form.copies}
                onChange={(e) => setForm({ ...form, copies: e.target.value })} className={inputCls} />
            </label>
            <label className="block text-sm font-medium text-slate-700 sm:col-span-3">
              Motif de la demande
              <textarea required minLength={5} maxLength={1000} rows={3} value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })} className={inputCls}
                placeholder="Ex. dossier de bourse, stage, visa…" />
            </label>
            {other && (
              <label className="block text-sm font-medium text-slate-700 sm:col-span-3">
                Précisions
                <textarea required maxLength={1000} rows={2} value={form.details}
                  onChange={(e) => setForm({ ...form, details: e.target.value })} className={inputCls}
                  placeholder="Décrivez le document dont vous avez besoin" />
              </label>
            )}
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Envoi..." : "Envoyer la demande"}
          </button>
        </form>
      )}

      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && items.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Vous n&apos;avez pas encore fait de demande.
        </p>
      )}
      <div className="mt-6 space-y-4">
        {items.map((r) => (
          <article key={r.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">{r.type}</h2>
                <p className="text-xs text-slate-500">
                  {r.copies} {r.copies > 1 ? "exemplaires" : "exemplaire"} · {formatDate(r.createdAt)}
                </p>
              </div>
              {r.status === "FILED" && (
                <button onClick={() => withdraw(r)} className={`${smallBtn} text-red-600`}>Retirer la demande</button>
              )}
            </div>
            <p className="mt-3 text-sm text-slate-700">{r.reason}</p>
            {r.details && <p className="mt-1 text-sm text-slate-500">{r.details}</p>}
            <Steps status={r.status} />
            {r.adminNote && (
              <p className="mt-4 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                <span className="font-semibold">Note de l&apos;administration :</span> {r.adminNote}
              </p>
            )}
          </article>
        ))}
      </div>
    </>
  );
}