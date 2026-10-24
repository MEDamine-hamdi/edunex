"use client";

import { InlineLoader } from "@/components/Loading";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "./ui";

type Audience = "STUDENT" | "PROFESSOR" | "BOTH";
type DocType = { id: number; name: string; audience: Audience; active: boolean; requiresDetails: boolean; maxCopies: number };

const AUDIENCE: Record<Audience, string> = { STUDENT: "Étudiants", PROFESSOR: "Professeurs", BOTH: "Étudiants et professeurs" };
const EMPTY = { id: null as number | null, name: "", audience: "STUDENT" as Audience, active: true, requiresDetails: false, maxCopies: "3" };

export function RequestTypesManager() {
  const [types, setTypes] = useState<DocType[]>([]);
  const [form, setForm] = useState(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setTypes(await apiFetch<DocType[]>("/api/admin/request-types"));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les types de documents." : e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const body = () => JSON.stringify({ name: form.name.trim(), audience: form.audience, active: form.active, requiresDetails: form.requiresDetails, maxCopies: Number(form.maxCopies) });

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await apiFetch(form.id ? `/api/admin/request-types/${form.id}` : "/api/admin/request-types", { method: form.id ? "PUT" : "POST", body: body() });
      setForm(EMPTY);
      setShowForm(false);
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 409 ? "Ce type de document existe déjà." : err.message);
    } finally {
      setSaving(false);
    }
  }

  async function toggle(t: DocType) {
    try {
      await apiFetch(`/api/admin/request-types/${t.id}`, {
        method: "PUT",
        body: JSON.stringify({ name: t.name, audience: t.audience, active: !t.active, requiresDetails: t.requiresDetails, maxCopies: t.maxCopies }),
      });
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function remove(t: DocType) {
    if (!window.confirm(`Supprimer « ${t.name} » ? Les demandes déjà faites sont conservées.`)) return;
    try {
      await apiFetch<void>(`/api/admin/request-types/${t.id}`, { method: "DELETE" });
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  function edit(t: DocType) {
    setForm({ id: t.id, name: t.name, audience: t.audience, active: t.active, requiresDetails: t.requiresDetails, maxCopies: String(t.maxCopies) });
    setShowForm(true);
  }

  return (
    <>
      <PageHeader
        title="Types de documents"
        subtitle="Les documents que les étudiants et les professeurs peuvent demander à l'administration."
        action={<button onClick={() => { setForm(EMPTY); setShowForm((v) => !v); }} className={primaryBtn}>{showForm ? "Fermer" : "+ Nouveau type"}</button>}
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Nom du document" maxLength={120} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <label className="block text-sm font-medium text-slate-700">
              Qui peut le demander
              <select value={form.audience} onChange={(e) => setForm({ ...form, audience: e.target.value as Audience })} className={inputCls}>
                {(Object.keys(AUDIENCE) as Audience[]).map((a) => <option key={a} value={a}>{AUDIENCE[a]}</option>)}
              </select>
            </label>
            <TextField label="Exemplaires maximum" type="number" min={1} max={10} value={form.maxCopies} onChange={(e) => setForm({ ...form, maxCopies: e.target.value })} required />
          </div>
          <div className="mt-4 flex flex-wrap gap-6 text-sm text-slate-700">
            <label className="flex items-center gap-2"><input type="checkbox" className="h-4 w-4 accent-blue-600" checked={form.requiresDetails} onChange={(e) => setForm({ ...form, requiresDetails: e.target.checked })} /> Demander des précisions au demandeur</label>
            <label className="flex items-center gap-2"><input type="checkbox" className="h-4 w-4 accent-blue-600" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Actif (demandable)</label>
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>{saving ? "Enregistrement..." : form.id ? "Enregistrer" : "Créer"}</button>
        </form>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Document</th>
              <th className="px-5 py-3">Demandeurs</th>
              <th className="px-5 py-3">Exemplaires max.</th>
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500"><InlineLoader /></td></tr>}
            {types.map((t) => (
              <tr key={t.id}>
                <td className="px-5 py-3 font-medium text-slate-900">{t.name}{t.requiresDetails && <span className="ms-2 text-xs font-normal text-slate-500">(précisions demandées)</span>}</td>
                <td className="px-5 py-3 text-slate-600">{AUDIENCE[t.audience]}</td>
                <td className="px-5 py-3 text-slate-600">{t.maxCopies}</td>
                <td className="px-5 py-3">
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${t.active ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"}`}>{t.active ? "Actif" : "Inactif"}</span>
                </td>
                <td className="px-5 py-3 text-right">
                  <div className="flex justify-end gap-2">
                    <button onClick={() => edit(t)} className={smallBtn}>Modifier</button>
                    <button onClick={() => toggle(t)} className={smallBtn}>{t.active ? "Désactiver" : "Activer"}</button>
                    <button onClick={() => remove(t)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}