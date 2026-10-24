"use client";

import { SectionLoader } from "@/components/Loading";
import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, inputCls, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type GradeType = "EXAM" | "QUIZ" | "ASSIGNMENT" | "PROJECT" | "PRACTICAL";
type SubjectAverage = {
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  coefficient: number | null;
  average: number | null;
  gradeCount: number;
};
type Summary = { subjects: SubjectAverage[]; overallAverage: number | null };
type Grade = {
  id: number;
  subjectId: number;
  subjectName: string;
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

const tone = (v: number) => (v < 10 ? "text-red-600" : "text-emerald-700");

export default function StudentGradesPage() {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [grades, setGrades] = useState<Grade[]>([]);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiFetch<Summary>("/api/student/grades/summary"),
      apiFetch<Grade[]>("/api/student/grades"),
    ])
      .then(([s, g]) => {
        setSummary(s);
        setGrades(g);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  const visible = filter ? grades.filter((g) => String(g.subjectId) === filter) : grades;
  const subjects = summary?.subjects ?? [];

  return (
    <StudentShell>
      <PageHeader title="Mes notes" subtitle="Vos notes et vos moyennes (sur 20)." />
      <ErrorBox message={error} />
      {loading && <SectionLoader />}
      {!loading && subjects.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune note pour le moment. Vos notes apparaîtront ici dès que l&apos;administration les aura saisies.
        </p>
      )}

      {summary && subjects.length > 0 && (
        <>
          <section className="mt-6 flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Moyenne générale</h2>
              <p className="mt-1 text-xs text-slate-500">Pondérée par les coefficients des matières notées.</p>
            </div>
            <p className={`text-4xl font-extrabold ${summary.overallAverage == null ? "text-slate-400" : tone(summary.overallAverage)}`}>
              {summary.overallAverage == null ? "-" : `${summary.overallAverage.toFixed(2)} / 20`}
            </p>
          </section>

          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {subjects.map((s) => (
              <section key={s.subjectId} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
                <span className="rounded-md bg-sky-100 px-2 py-1 text-xs font-bold text-sky-700">{s.subjectCode}</span>
                <h2 className="mt-3 text-lg font-semibold">{s.subjectName}</h2>
                <p className={`mt-3 text-3xl font-extrabold ${s.average == null ? "text-slate-400" : tone(s.average)}`}>
                  {s.average == null ? "-" : `${s.average.toFixed(2)} / 20`}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  Coefficient {s.coefficient ?? "-"} · {s.gradeCount} note{s.gradeCount > 1 ? "s" : ""}
                </p>
              </section>
            ))}
          </div>
        </>
      )}

      {grades.length > 0 && (
        <>
          <div className="mt-10 flex flex-wrap items-center justify-between gap-4">
            <h2 className="text-lg font-semibold">Détail des notes</h2>
            {subjects.length > 1 && (
              <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${inputCls} mt-0 w-64`}>
                <option value="">Toutes les matières</option>
                {subjects.map((s) => (
                  <option key={s.subjectId} value={s.subjectId}>{s.subjectName}</option>
                ))}
              </select>
            )}
          </div>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr>
                  <th className="px-5 py-3">Date</th>
                  <th className="px-5 py-3">Matière</th>
                  <th className="px-5 py-3">Type</th>
                  <th className="px-5 py-3">Note</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visible.map((g) => (
                  <tr key={g.id}>
                    <td className="px-5 py-3 text-slate-700">{fmt(g.date)}</td>
                    <td className="px-5 py-3 text-slate-700">{g.subjectName}</td>
                    <td className="px-5 py-3 text-slate-600">{TYPES[g.evaluationType]}</td>
                    <td className={`px-5 py-3 font-semibold ${tone(g.value)}`}>{g.value} / 20</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </StudentShell>
  );
}