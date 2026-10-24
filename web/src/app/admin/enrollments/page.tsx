"use client";

import { InlineLoader } from "@/components/Loading";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError, apiFetch } from "@/lib/api";
import {
  ErrorBox,
  PageHeader,
  inputCls,
  primaryBtn,
  smallBtn,
  tableWrap,
  theadCls,
} from "@/components/ui";

type EnrollmentStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";
type Enrollment = {
  id: number;
  studentName: string;
  studentNumber: string;
  className: string;
  sectionName: string | null;
  academicYear: string;
  enrollmentDate: string;
  status: EnrollmentStatus;
};
type Student = { id: number; firstName: string; lastName: string; studentNumber: string; status: string };
type ClassItem = { id: number; name: string; academicYear: string };
type Section = { id: number; name: string };

const statusStyle: Record<EnrollmentStatus, { label: string; cls: string }> = {
  ACTIVE: { label: "Active", cls: "bg-emerald-100 text-emerald-700" },
  COMPLETED: { label: "Terminée", cls: "bg-slate-200 text-slate-600" },
  CANCELLED: { label: "Annulée", cls: "bg-red-100 text-red-700" },
};

export default function EnrollmentsPage() {
  const [rows, setRows] = useState<Enrollment[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ studentId: "", classId: "", sectionId: "" });
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      const [e, s, c] = await Promise.all([
        apiFetch<Enrollment[]>("/api/admin/enrollments"),
        apiFetch<Student[]>("/api/admin/students"),
        apiFetch<ClassItem[]>("/api/admin/classes"),
      ]);
      setRows(e);
      setStudents(s.filter((x) => x.status === "ACTIVE"));
      setClasses(c);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError && err.status !== 401) setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function onClassChange(classId: string) {
    setForm((f) => ({ ...f, classId, sectionId: "" }));
    setSections([]);
    if (!classId) return;
    try {
      setSections(await apiFetch<Section[]>(`/api/admin/classes/${classId}/sections`));
    } catch {
      /* sections are optional */
    }
  }

  async function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await apiFetch<Enrollment>("/api/admin/enrollments", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(form.studentId),
          classId: Number(form.classId),
          sectionId: form.sectionId ? Number(form.sectionId) : null,
        }),
      });
      setRows((prev) => [...prev, created]);
      setForm({ studentId: "", classId: "", sectionId: "" });
      setSections([]);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409)
          setError(
            err.message.includes("already")
              ? "Cet étudiant est déjà inscrit dans cette classe pour cette année."
              : "Le groupe n'appartient pas à cette classe."
          );
        else setError(err.message);
      }
    } finally {
      setSaving(false);
    }
  }

  async function toggle(row: Enrollment) {
    const next: EnrollmentStatus = row.status === "ACTIVE" ? "CANCELLED" : "ACTIVE";
    setError(null);
    try {
      const updated = await apiFetch<Enrollment>(`/api/admin/enrollments/${row.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: next }),
      });
      setRows((prev) => prev.map((r) => (r.id === row.id ? updated : r)));
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 409 ? "Cet étudiant a déjà une inscription active dans cette classe pour cette année." : err.message);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Inscriptions"
        subtitle="Inscrivez les étudiants dans une classe et une section."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouvelle inscription"}
          </button>
        }
      />
      <ErrorBox message={error} />

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="block text-sm font-medium text-slate-700">
              Étudiant
              <select
                required
                value={form.studentId}
                onChange={(e) => setForm({ ...form, studentId: e.target.value })}
                className={inputCls}
              >
                <option value="">Choisir...</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.firstName} {s.lastName} ({s.studentNumber})
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Classe
              <select
                required
                value={form.classId}
                onChange={(e) => onClassChange(e.target.value)}
                className={inputCls}
              >
                <option value="">Choisir...</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.academicYear})
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Groupe (optionnel)
              <select
                value={form.sectionId}
                onChange={(e) => setForm({ ...form, sectionId: e.target.value })}
                disabled={sections.length === 0}
                className={inputCls}
              >
                <option value="">Aucune</option>
                {sections.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </label>
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Inscription..." : "Inscrire l'étudiant"}
          </button>
        </form>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Étudiant</th>
              <th className="px-5 py-3">Classe</th>
              <th className="px-5 py-3">Groupe</th>
              <th className="px-5 py-3">Année</th>
              <th className="px-5 py-3">Date</th>
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr><td colSpan={7} className="px-5 py-8 text-center text-slate-500"><InlineLoader /></td></tr>
            )}
            {!loading && rows.length === 0 && (
              <tr><td colSpan={7} className="px-5 py-8 text-center text-slate-500">Aucune inscription pour le moment.</td></tr>
            )}
            {rows.map((r) => {
              const s = statusStyle[r.status];
              return (
                <tr key={r.id}>
                  <td className="px-5 py-4">
                    <span className="font-medium text-slate-900">{r.studentName}</span>
                    <span className="block text-xs text-slate-500">{r.studentNumber}</span>
                  </td>
                  <td className="px-5 py-4 text-slate-600">{r.className}</td>
                  <td className="px-5 py-4 text-slate-600">{r.sectionName ?? "-"}</td>
                  <td className="px-5 py-4 text-slate-600">{r.academicYear}</td>
                  <td className="px-5 py-4 text-slate-600">{r.enrollmentDate}</td>
                  <td className="px-5 py-4">
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${s.cls}`}>{s.label}</span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button onClick={() => toggle(r)} className={smallBtn}>
                      {r.status === "ACTIVE" ? "Annuler" : "Réactiver"}
                    </button>
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