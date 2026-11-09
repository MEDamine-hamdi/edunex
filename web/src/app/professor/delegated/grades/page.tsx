"use client";

import { FormEvent, useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Subject = { id: number; name: string; code: string };
type Student = { studentId: number; studentName: string; studentNumber: string };
type Done = { key: number; student: string; subject: string; type: string; value: string; date: string };

export default function DelegatedGradesPage() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [types, setTypes] = useState<string[]>([]);
  const [subjectId, setSubjectId] = useState("");
  const [students, setStudents] = useState<Student[]>([]);
  const [studentId, setStudentId] = useState("");
  const [type, setType] = useState("");
  const [value, setValue] = useState("");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [done, setDone] = useState<Done[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([apiFetch<Subject[]>("/api/delegated/grades/subjects"), apiFetch<string[]>("/api/delegated/grades/evaluation-types")])
      .then(([s, t]) => {
        setSubjects(s);
        setTypes(t);
        if (t[0]) setType(t[0]);
      })
      .catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.status === 403 ? "Vous n'avez pas le droit de saisir des notes." : e.message));
  }, []);

  useEffect(() => {
    setStudents([]);
    setStudentId("");
    if (!subjectId) return;
    apiFetch<Student[]>(`/api/delegated/grades/subjects/${subjectId}/students`).then(setStudents).catch((e) => e instanceof ApiError && setError(e.message));
  }, [subjectId]);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await apiFetch("/api/delegated/grades", {
        method: "POST",
        body: JSON.stringify({ studentId: Number(studentId), subjectId: Number(subjectId), value: Number(value), evaluationType: type, date }),
      });
      const st = students.find((s) => String(s.studentId) === studentId);
      const su = subjects.find((s) => String(s.id) === subjectId);
      setDone((d) => [{ key: Date.now(), student: st?.studentName ?? "", subject: su?.name ?? "", type, value, date }, ...d]);
      setValue("");
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 400 ? "Note refusée : vérifiez la valeur (0 à 20), la date (pas dans le futur) et que l'étudiant est inscrit." : err.status === 403 ? "Vous n'enseignez pas cette matière." : err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ProfessorShell>
      <PageHeader title="Saisie des notes" subtitle="Droit accordé par l'administration : vous pouvez saisir des notes dans les matières que vous enseignez. L'étudiant est prévenu à chaque note." />
      <ErrorBox message={error} />
      <form onSubmit={submit} className="mt-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2 lg:grid-cols-3">
        <label className="block text-sm font-medium text-slate-700">Matière
          <select required value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className={inputCls}><option value="">Choisir...</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Étudiant
          <select required value={studentId} onChange={(e) => setStudentId(e.target.value)} className={inputCls}><option value="">Choisir...</option>{students.map((s) => <option key={s.studentId} value={s.studentId}>{s.studentName} ({s.studentNumber})</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Type d&apos;évaluation
          <select required value={type} onChange={(e) => setType(e.target.value)} className={inputCls}>{types.map((t) => <option key={t} value={t}>{t}</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Note (sur 20)
          <input type="number" required min={0} max={20} step={0.25} value={value} onChange={(e) => setValue(e.target.value)} className={inputCls} />
        </label>
        <label className="block text-sm font-medium text-slate-700">Date
          <input type="date" required max={new Date().toISOString().slice(0, 10)} value={date} onChange={(e) => setDate(e.target.value)} className={inputCls} />
        </label>
        <div className="flex items-end"><button type="submit" disabled={busy} className={primaryBtn}>{busy ? "Enregistrement..." : "Enregistrer la note"}</button></div>
      </form>

      <h2 className="mt-8 text-lg font-semibold">Notes saisies pendant cette session</h2>
      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}><tr><th className="px-5 py-3">Étudiant</th><th className="px-5 py-3">Matière</th><th className="px-5 py-3">Type</th><th className="px-5 py-3">Note</th><th className="px-5 py-3">Date</th></tr></thead>
          <tbody className="divide-y divide-slate-100">
            {done.length === 0 && <tr><td colSpan={5} className="px-5 py-6 text-center text-slate-500">Aucune note saisie pour le moment.</td></tr>}
            {done.map((d) => <tr key={d.key}><td className="px-5 py-3 font-medium text-slate-900">{d.student}</td><td className="px-5 py-3 text-slate-700">{d.subject}</td><td className="px-5 py-3 text-slate-700">{d.type}</td><td className="px-5 py-3 text-slate-700">{d.value}/20</td><td className="px-5 py-3 text-slate-700">{d.date}</td></tr>)}
          </tbody>
        </table>
      </div>
    </ProfessorShell>
  );
}