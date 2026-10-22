"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type ClassItem = { id: number; name: string; level: string | null; academicYear: string; subjects: { id: number; name: string; code: string }[] };

export default function ClassesPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [form, setForm] = useState({ name: "", level: "", academicYear: "" });
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setClasses(await apiFetch<ClassItem[]>("/api/admin/classes"));
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

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await apiFetch("/api/admin/classes", {
        method: "POST",
        body: JSON.stringify({ name: form.name.trim(), level: form.level.trim() || null, academicYear: form.academicYear.trim() }),
      });
      setForm({ name: "", level: "", academicYear: "" });
      setShowForm(false);
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Classes"
        subtitle="Créez les classes, puis ouvrez « Gérer la classe » pour leurs étudiants, leurs professeurs, leurs matières et leurs groupes."
        action={<button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>{showForm ? "Fermer" : "+ Nouvelle classe"}</button>}
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={create} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Nom" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <TextField label="Niveau (ex. L2)" value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })} />
            <TextField label="Année (ex. 2026-2027)" value={form.academicYear} onChange={(e) => setForm({ ...form, academicYear: e.target.value })} required />
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>{saving ? "Création..." : "Créer la classe"}</button>
        </form>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Classe</th>
              <th className="px-5 py-3">Niveau</th>
              <th className="px-5 py-3">Année</th>
              <th className="px-5 py-3">Matières</th>
              <th className="px-5 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {!loading && classes.length === 0 && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucune classe.</td></tr>}
            {classes.map((c) => (
              <tr key={c.id}>
                <td className="px-5 py-4 font-medium text-slate-900">{c.name}</td>
                <td className="px-5 py-4 text-slate-600">{c.level ?? "-"}</td>
                <td className="px-5 py-4 text-slate-600">{c.academicYear}</td>
                <td className="px-5 py-4">
                  <div className="flex flex-wrap gap-1.5">
                    {c.subjects.length === 0 && <span className="text-slate-400">-</span>}
                    {c.subjects.map((s) => (
                      <span key={s.id} className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs font-bold text-emerald-700">{s.code}</span>
                    ))}
                  </div>
                </td>
                <td className="px-5 py-4 text-right">
                  <Link href={`/admin/classes/${c.id}`} className={primaryBtn}>Gérer la classe</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}