"use client";

import { FormEvent, Fragment, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError, apiFetch } from "@/lib/api";
import {
  ErrorBox,
  IdPicker,
  PageHeader,
  TextField,
  inputCls,
  primaryBtn,
  smallBtn,
  tableWrap,
  theadCls,
} from "@/components/ui";

type SubjectRef = { id: number; name: string; code: string };
type ClassItem = { id: number; name: string; level: string | null; academicYear: string; subjects: SubjectRef[] };
type Section = { id: number; name: string; classId: number };

const emptyForm = { name: "", level: "", academicYear: "" };

export default function ClassesPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [openId, setOpenId] = useState<number | null>(null);
  const [mode, setMode] = useState<"subjects" | "sections">("subjects");
  const [sections, setSections] = useState<Section[]>([]);
  const [newSection, setNewSection] = useState("");

  const load = useCallback(async () => {
    try {
      const [c, s] = await Promise.all([
        apiFetch<ClassItem[]>("/api/admin/classes"),
        apiFetch<SubjectRef[]>("/api/admin/subjects"),
      ]);
      setClasses(c);
      setSubjects(s);
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
      const created = await apiFetch<ClassItem>("/api/admin/classes", {
        method: "POST",
        body: JSON.stringify({
          name: form.name.trim(),
          level: form.level.trim() || null,
          academicYear: form.academicYear.trim(),
        }),
      });
      setClasses((prev) => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)));
      setForm(emptyForm);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function openPanel(id: number, m: "subjects" | "sections") {
    if (openId === id && mode === m) {
      setOpenId(null);
      return;
    }
    setOpenId(id);
    setMode(m);
    setError(null);
    setNewSection("");
    if (m === "sections") {
      try {
        setSections(await apiFetch<Section[]>(`/api/admin/classes/${id}/sections`));
      } catch (err) {
        if (err instanceof ApiError) setError(err.message);
      }
    }
  }

  async function assignSubjects(id: number, ids: number[]) {
    setSaving(true);
    setError(null);
    try {
      const updated = await apiFetch<ClassItem>(`/api/admin/classes/${id}/subjects`, {
        method: "PUT",
        body: JSON.stringify({ ids }),
      });
      setClasses((prev) => prev.map((c) => (c.id === id ? updated : c)));
      setOpenId(null);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function addSection(classId: number) {
    if (!newSection.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await apiFetch<Section>(`/api/admin/classes/${classId}/sections`, {
        method: "POST",
        body: JSON.stringify({ name: newSection.trim() }),
      });
      setSections((prev) => [...prev, created]);
      setNewSection("");
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
        subtitle="Créez les classes, rattachez-leur des matières et gérez leurs sections."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouvelle classe"}
          </button>
        }
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Nom" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <TextField label="Niveau" value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })} />
            <TextField label="Année universitaire (ex. 2026-2027)" value={form.academicYear} onChange={(e) => setForm({ ...form, academicYear: e.target.value })} required />
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Création..." : "Créer la classe"}
          </button>
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
            {loading && (
              <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>
            )}
            {!loading && classes.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucune classe pour le moment.</td></tr>
            )}
            {classes.map((c) => (
              <Fragment key={c.id}>
                <tr>
                  <td className="px-5 py-4 font-medium text-slate-900">{c.name}</td>
                  <td className="px-5 py-4 text-slate-600">{c.level ?? "-"}</td>
                  <td className="px-5 py-4 text-slate-600">{c.academicYear}</td>
                  <td className="px-5 py-4 text-slate-600">
                    {c.subjects.length ? c.subjects.map((s) => s.code).join(", ") : "-"}
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => openPanel(c.id, "subjects")} className={smallBtn}>Matières</button>
                      <button onClick={() => openPanel(c.id, "sections")} className={smallBtn}>Sections</button>
                    </div>
                  </td>
                </tr>
                {openId === c.id && (
                  <tr className="bg-slate-50">
                    <td colSpan={5} className="px-5 py-5">
                      {mode === "subjects" ? (
                        <>
                          <p className="mb-3 text-sm font-medium">Matières de {c.name}</p>
                          <IdPicker
                            options={subjects.map((s) => ({ id: s.id, label: `${s.name} (${s.code})` }))}
                            selected={c.subjects.map((s) => s.id)}
                            onSave={(ids) => assignSubjects(c.id, ids)}
                            saving={saving}
                            emptyText="Aucune matière. Créez-en d&apos;abord dans la page Matières."
                          />
                        </>
                      ) : (
                        <>
                          <p className="mb-3 text-sm font-medium">Sections de {c.name}</p>
                          <div className="flex flex-wrap gap-2">
                            {sections.length === 0 && <span className="text-sm text-slate-500">Aucune section.</span>}
                            {sections.map((s) => (
                              <span key={s.id} className="rounded-full bg-white px-3 py-1.5 text-sm font-medium text-slate-700 ring-1 ring-slate-200">
                                {s.name}
                              </span>
                            ))}
                          </div>
                          <div className="mt-4 flex max-w-md gap-3">
                            <input
                              value={newSection}
                              onChange={(e) => setNewSection(e.target.value)}
                              placeholder="Nom de la section (ex. Groupe B)"
                              className={`${inputCls} mt-0`}
                            />
                            <button onClick={() => addSection(c.id)} disabled={saving} className={primaryBtn}>
                              Ajouter
                            </button>
                          </div>
                        </>
                      )}
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