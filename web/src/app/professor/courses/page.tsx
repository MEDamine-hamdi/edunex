"use client";

import { SectionLoader } from "@/components/Loading";
import { ChangeEvent, FormEvent, useCallback, useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { DocumentRow } from "@/components/CourseParts";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Course, SubjectRef } from "@/lib/types";

const ACCEPT = ".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.odt,.odp,.txt,.md,.png,.jpg,.jpeg";

export default function ProfessorCoursesPage() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: "", description: "", subjectId: "" });
  const [saving, setSaving] = useState(false);
  const [uploadingId, setUploadingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ title: "", description: "" });

  const load = useCallback(async () => {
    try {
      const [c, s] = await Promise.all([
        apiFetch<Course[]>("/api/professor/courses"),
        apiFetch<SubjectRef[]>("/api/professor/subjects"),
      ]);
      setCourses(c);
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

  const replace = (updated: Course) =>
    setCourses((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));

  async function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await apiFetch<Course>("/api/professor/courses", {
        method: "POST",
        body: JSON.stringify({
          title: form.title.trim(),
          description: form.description.trim() || null,
          subjectId: Number(form.subjectId),
        }),
      });
      setCourses((prev) => [created, ...prev]);
      setForm({ title: "", description: "", subjectId: "" });
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function saveEdit(id: number) {
    setError(null);
    try {
      const updated = await apiFetch<Course>(`/api/professor/courses/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          title: editForm.title.trim(),
          description: editForm.description.trim() || null,
        }),
      });
      replace(updated);
      setEditingId(null);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function removeCourse(c: Course) {
    if (!window.confirm(`Supprimer le cours « ${c.title} » et ses documents ?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/professor/courses/${c.id}`, { method: "DELETE" });
      setCourses((prev) => prev.filter((x) => x.id !== c.id));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function upload(courseId: number, e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploadingId(courseId);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      replace(await apiFetch<Course>(`/api/professor/courses/${courseId}/documents`, { method: "POST", body: fd }));
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 400) setError("Type de fichier non supporté (PDF, Word, PowerPoint, Excel, texte, images).");
        else if (err.status === 413) setError("Fichier trop volumineux (25 Mo maximum).");
        else setError(err.message);
      }
    } finally {
      setUploadingId(null);
    }
  }

  async function removeDocument(id: number) {
    if (!window.confirm("Supprimer ce document ?")) return;
    setError(null);
    try {
      replace(await apiFetch<Course>(`/api/professor/documents/${id}`, { method: "DELETE" }));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <ProfessorShell>
      <PageHeader
        title="Mes cours"
        subtitle="Créez vos cours et déposez les documents pour vos étudiants."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouveau cours"}
          </button>
        }
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {subjects.length === 0 && (
            <p className="mb-4 rounded-xl bg-orange-50 px-4 py-3 text-sm text-orange-700">
              Aucune matière ne vous est affectée : demandez à l&apos;administration de vous en affecter une.
            </p>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Titre" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            <label className="block text-sm font-medium text-slate-700">
              Matière
              <select
                required
                value={form.subjectId}
                onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
                className={inputCls}
              >
                <option value="">Choisir...</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700 sm:col-span-2">
              Description
              <textarea
                rows={3}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className={inputCls}
              />
            </label>
          </div>
          <button type="submit" disabled={saving || subjects.length === 0} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Création..." : "Créer le cours"}
          </button>
        </form>
      )}

      {loading && <SectionLoader />}
      {!loading && courses.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Vous n&apos;avez pas encore de cours.
        </p>
      )}

      <div className="mt-6 space-y-5">
        {courses.map((c) => (
          <article key={c.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            {editingId === c.id ? (
              <div className="space-y-3">
                <input
                  value={editForm.title}
                  onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                  className={`${inputCls} mt-0`}
                />
                <textarea
                  rows={2}
                  value={editForm.description}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                  className={`${inputCls} mt-0`}
                />
                <div className="flex gap-2">
                  <button onClick={() => saveEdit(c.id)} className={primaryBtn}>Enregistrer</button>
                  <button onClick={() => setEditingId(null)} className={smallBtn}>Annuler</button>
                </div>
              </div>
            ) : (
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <span className="rounded-md bg-emerald-100 px-2 py-1 text-xs font-bold text-emerald-700">
                    {c.subject.code} · {c.subject.name}
                  </span>
                  <h2 className="mt-3 text-lg font-semibold">{c.title}</h2>
                  {c.description && <p className="mt-1 text-sm text-slate-600">{c.description}</p>}
                  <p className="mt-2 text-xs text-slate-500">Créé le {formatDate(c.createdAt)}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      setEditingId(c.id);
                      setEditForm({ title: c.title, description: c.description ?? "" });
                    }}
                    className={smallBtn}
                  >
                    Modifier
                  </button>
                  <button onClick={() => removeCourse(c)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                </div>
              </div>
            )}

            <h3 className="mb-3 mt-6 text-sm font-semibold text-slate-700">
              Documents ({c.documents.length})
            </h3>
            <ul className="space-y-2">
              {c.documents.map((d) => (
                <DocumentRow key={d.id} doc={d} onDelete={() => removeDocument(d.id)} onError={setError} />
              ))}
            </ul>
            <label className={`mt-4 inline-block cursor-pointer ${smallBtn}`}>
              {uploadingId === c.id ? "Envoi en cours..." : "+ Ajouter un document"}
              <input
                type="file"
                hidden
                accept={ACCEPT}
                disabled={uploadingId === c.id}
                onChange={(e) => upload(c.id, e)}
              />
            </label>
          </article>
        ))}
      </div>
    </ProfessorShell>
  );
}