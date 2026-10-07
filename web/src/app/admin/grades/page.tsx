"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type GradeType = "EXAM" | "QUIZ" | "ASSIGNMENT" | "PROJECT" | "PRACTICAL";
type SubjectItem = { id: number; name: string; code: string; coefficient: number | null };
type RosterEntry = { studentId: number; studentName: string; studentNumber: string; className: string };
type Grade = {
  id: number;
  studentId: number;
  studentName: string;
  studentNumber: string;
  value: number;
  evaluationType: GradeType;
  date: string;
};

const TYPES: Record<GradeType, string> = {
  EXAM: "Examen",
  QUIZ: "Quiz",
  ASSIGNMENT: "Devoir",
  PROJECT: "Projet",
  PRACTICAL: "TP",
};

const fmt = (d: string) =>
  new Date(d + "T00:00:00").toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });

function todayStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const emptyForm = () => ({ studentId: "", value: "", evaluationType: "EXAM" as GradeType, date: todayStr() });

function explain(err: ApiError): string {
  const m = err.message;
  if (m.includes("not enrolled")) return "Cet étudiant n'est pas inscrit dans une classe qui suit cette matière.";
  if (m.includes("future")) return "La date ne peut pas être dans le futur.";
  if (m.includes("value")) return "La note doit être comprise entre 0 et 20.";
  return m;
}

export default function AdminGradesPage() {
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [subjectId, setSubjectId] = useState("");
  const [roster, setRoster] = useState<RosterEntry[]>([]);
  const [grades, setGrades] = useState<Grade[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    apiFetch<SubjectItem[]>("/api/admin/subjects")
      .then((s) => {
        setSubjects(s);
        if (s[0]) setSubjectId(String(s[0].id));
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  const loadSubject = useCallback(async (sid: string) => {
    setError(null);
    try {
      const [r, g] = await Promise.all([
        apiFetch<RosterEntry[]>(`/api/admin/subjects/${sid}/students`),
        apiFetch<Grade[]>(`/api/admin/grades?subjectId=${sid}`),
      ]);
      setRoster(r);
      setGrades(g);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, []);

  useEffect(() => {
    if (subjectId) {
      setShowForm(false);
      setEditingId(null);
      loadSubject(subjectId);
    }
  }, [subjectId, loadSubject]);

  const subject = subjects.find((s) => String(s.id) === subjectId);

  const averages = useMemo(() => {
    const map = new Map<number, { name: string; number: string; sum: number; n: number }>();
    grades.forEach((g) => {
      const cur = map.get(g.studentId) ?? { name: g.studentName, number: g.studentNumber, sum: 0, n: 0 };
      cur.sum += g.value;
      cur.n += 1;
      map.set(g.studentId, cur);
    });
    return [...map.entries()]
      .map(([id, v]) => ({ id, name: v.name, number: v.number, n: v.n, avg: v.sum / v.n }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [grades]);

  function openNew() {
    setEditingId(null);
    setForm(emptyForm());
    setFormError(null);
    setShowForm(true);
  }

  function startEdit(g: Grade) {
    setEditingId(g.id);
    setForm({ studentId: String(g.studentId), value: String(g.value), evaluationType: g.evaluationType, date: g.date });
    setFormError(null);
    setShowForm(true);
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      if (editingId) {
        await apiFetch<Grade>(`/api/admin/grades/${editingId}`, {
          method: "PUT",
          body: JSON.stringify({ value: Number(form.value), evaluationType: form.evaluationType, date: form.date }),
        });
        setShowForm(false);
        setEditingId(null);
      } else {
        await apiFetch<Grade>("/api/admin/grades", {
          method: "POST",
          body: JSON.stringify({
            studentId: Number(form.studentId),
            subjectId: Number(subjectId),
            value: Number(form.value),
            evaluationType: form.evaluationType,
            date: form.date,
          }),
        });
        setForm((f) => ({ ...f, studentId: "", value: "" }));
      }
      await loadSubject(subjectId);
    } catch (err) {
      if (err instanceof ApiError) setFormError(explain(err));
    } finally {
      setSaving(false);
    }
  }

  async function remove(g: Grade) {
    if (!window.confirm(`Supprimer la note ${g.value}/20 de ${g.studentName} ?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/admin/grades/${g.id}`, { method: "DELETE" });
      setGrades((prev) => prev.filter((x) => x.id !== g.id));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Notes"
        subtitle="Saisissez et modifiez les notes (sur 20) des étudiants, matière par matière."
        action={
          subjectId ? (
            <button onClick={showForm ? () => setShowForm(false) : openNew} className={primaryBtn}>
              {showForm ? "Fermer" : "+ Nouvelle note"}
            </button>
          ) : undefined
        }
      />
      <ErrorBox message={error} />

      {!loading && subjects.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune matière. Créez-en d&apos;abord dans la page Matières.
        </p>
      )}

      {subjects.length > 0 && (
        <label className="mt-6 block text-sm font-medium text-slate-700">
          Matière
          <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className={`${inputCls} w-80`}>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
            ))}
          </select>
          {subject && <span className="mt-1 block text-xs text-slate-500">Coefficient : {subject.coefficient ?? "-"}</span>}
        </label>
      )}

      {showForm && (
        <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">{editingId ? "Modifier la note" : "Nouvelle note"}</h2>
          <ErrorBox message={formError} />
          {roster.length === 0 && !editingId && (
            <p className="mt-4 rounded-xl bg-orange-50 px-4 py-3 text-sm text-orange-700">
              Aucun étudiant inscrit dans une classe qui suit cette matière.
            </p>
          )}
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <label className="block text-sm font-medium text-slate-700">
              Étudiant
              <select
                required
                disabled={!!editingId}
                value={form.studentId}
                onChange={(e) => setForm({ ...form, studentId: e.target.value })}
                className={inputCls}
              >
                <option value="">Choisir...</option>
                {editingId
                  ? grades.filter((g) => String(g.studentId) === form.studentId).slice(0, 1).map((g) => (
                      <option key={g.studentId} value={g.studentId}>{g.studentName}</option>
                    ))
                  : roster.map((r) => (
                      <option key={r.studentId} value={r.studentId}>{r.studentName} ({r.studentNumber})</option>
                    ))}
              </select>
            </label>
            <TextField
              label="Note (sur 20)"
              type="number"
              step="0.25"
              min="0"
              max="20"
              value={form.value}
              onChange={(e) => setForm({ ...form, value: e.target.value })}
              required
            />
            <label className="block text-sm font-medium text-slate-700">
              Type
              <select
                value={form.evaluationType}
                onChange={(e) => setForm({ ...form, evaluationType: e.target.value as GradeType })}
                className={inputCls}
              >
                {(Object.keys(TYPES) as GradeType[]).map((t) => (
                  <option key={t} value={t}>{TYPES[t]}</option>
                ))}
              </select>
            </label>
            <TextField
              label="Date"
              type="date"
              max={todayStr()}
              value={form.date}
              onChange={(e) => setForm({ ...form, date: e.target.value })}
              required
            />
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Enregistrement..." : editingId ? "Enregistrer" : "Ajouter la note"}
          </button>
        </form>
      )}

      {subjectId && (
        <>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr>
                  <th className="px-5 py-3">Étudiant</th>
                  <th className="px-5 py-3">Type</th>
                  <th className="px-5 py-3">Note</th>
                  <th className="px-5 py-3">Date</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {grades.length === 0 && (
                  <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucune note pour cette matière.</td></tr>
                )}
                {grades.map((g) => (
                  <tr key={g.id}>
                    <td className="px-5 py-4">
                      <span className="font-medium text-slate-900">{g.studentName}</span>
                      <span className="block text-xs text-slate-500">{g.studentNumber}</span>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{TYPES[g.evaluationType]}</td>
                    <td className="px-5 py-4 font-semibold text-slate-900">{g.value} / 20</td>
                    <td className="px-5 py-4 text-slate-600">{fmt(g.date)}</td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button onClick={() => startEdit(g)} className={smallBtn}>Modifier</button>
                        <button onClick={() => remove(g)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {averages.length > 0 && (
            <>
              <h2 className="mt-10 text-lg font-semibold">Moyennes dans la matière</h2>
              <div className={tableWrap}>
                <table className="w-full text-left text-sm">
                  <thead className={theadCls}>
                    <tr>
                      <th className="px-5 py-3">Étudiant</th>
                      <th className="px-5 py-3">Notes</th>
                      <th className="px-5 py-3">Moyenne</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {averages.map((a) => (
                      <tr key={a.id}>
                        <td className="px-5 py-3 font-medium text-slate-900">{a.name}</td>
                        <td className="px-5 py-3 text-slate-600">{a.n}</td>
                        <td className={`px-5 py-3 font-semibold ${a.avg < 10 ? "text-red-600" : "text-emerald-700"}`}>
                          {a.avg.toFixed(2)} / 20
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </>
      )}
    </AdminShell>
  );
}