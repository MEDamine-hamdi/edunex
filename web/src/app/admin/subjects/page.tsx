"use client";

import { InlineLoader } from "@/components/Loading";
import { FormEvent, Fragment, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError, apiFetch } from "@/lib/api";
import {
  ErrorBox,
  IdPicker,
  PageHeader,
  TextField,
  primaryBtn,
  smallBtn,
  tableWrap,
  theadCls,
} from "@/components/ui";

type Person = { id: number; firstName: string; lastName: string };
type Subject = {
  id: number;
  name: string;
  code: string;
  coefficient: number | null;
  credits: number | null;
  professors: Person[];
};

const emptyForm = { name: "", code: "", coefficient: "", credits: "" };

export default function SubjectsPage() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [professors, setProfessors] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [openId, setOpenId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const [s, p] = await Promise.all([
        apiFetch<Subject[]>("/api/admin/subjects"),
        apiFetch<Person[]>("/api/admin/professors"),
      ]);
      setSubjects(s);
      setProfessors(p);
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

  async function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await apiFetch<Subject>("/api/admin/subjects", {
        method: "POST",
        body: JSON.stringify({
          name: form.name.trim(),
          code: form.code.trim(),
          coefficient: form.coefficient === "" ? null : Number(form.coefficient),
          credits: form.credits === "" ? null : Number(form.credits),
        }),
      });
      setSubjects((prev) => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)));
      setForm(emptyForm);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 409 ? "Ce code de matière est déjà utilisé." : err.message);
    } finally {
      setSaving(false);
    }
  }

  async function assign(id: number, ids: number[]) {
    setSaving(true);
    setError(null);
    try {
      const updated = await apiFetch<Subject>(`/api/admin/subjects/${id}/professors`, {
        method: "PUT",
        body: JSON.stringify({ ids }),
      });
      setSubjects((prev) => prev.map((s) => (s.id === id ? updated : s)));
      setOpenId(null);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Matières"
        subtitle="Créez les matières et affectez-leur des professeurs."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouvelle matière"}
          </button>
        }
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Nom" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <TextField label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} required />
            <TextField label="Coefficient" type="number" step="0.5" min="0" value={form.coefficient} onChange={(e) => setForm({ ...form, coefficient: e.target.value })} />
            <TextField label="Crédits" type="number" min="0" value={form.credits} onChange={(e) => setForm({ ...form, credits: e.target.value })} />
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Création..." : "Créer la matière"}
          </button>
        </form>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Matière</th>
              <th className="px-5 py-3">Code</th>
              <th className="px-5 py-3">Coef.</th>
              <th className="px-5 py-3">Crédits</th>
              <th className="px-5 py-3">Professeurs</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500"><InlineLoader /></td></tr>
            )}
            {!loading && subjects.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Aucune matière pour le moment.</td></tr>
            )}
            {subjects.map((s) => (
              <Fragment key={s.id}>
                <tr>
                  <td className="px-5 py-4 font-medium text-slate-900">{s.name}</td>
                  <td className="px-5 py-4 text-slate-600">{s.code}</td>
                  <td className="px-5 py-4 text-slate-600">{s.coefficient ?? "-"}</td>
                  <td className="px-5 py-4 text-slate-600">{s.credits ?? "-"}</td>
                  <td className="px-5 py-4 text-slate-600">
                    {s.professors.length ? s.professors.map((p) => `${p.firstName} ${p.lastName}`).join(", ") : "-"}
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button onClick={() => setOpenId(openId === s.id ? null : s.id)} className={smallBtn}>
                      Professeurs
                    </button>
                  </td>
                </tr>
                {openId === s.id && (
                  <tr className="bg-slate-50">
                    <td colSpan={6} className="px-5 py-5">
                      <p className="mb-3 text-sm font-medium">Professeurs de {s.name}</p>
                      <IdPicker
                        options={professors.map((p) => ({ id: p.id, label: `${p.firstName} ${p.lastName}` }))}
                        selected={s.professors.map((p) => p.id)}
                        onSave={(ids) => assign(s.id, ids)}
                        saving={saving}
                        emptyText="Aucun professeur. Créez-en d&apos;abord dans la page Professeurs."
                      />
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}