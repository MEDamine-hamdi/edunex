"use client";

import { SectionLoader } from "@/components/Loading";
import { useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type ClassItem = {
  id: number;
  name: string;
  level: string | null;
  academicYear: string;
  subjects: { id: number; name: string; code: string }[];
  students: { id: number; name: string; studentNumber: string; section: string | null }[];
};

export default function ProfessorClassesPage() {
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<ClassItem[]>("/api/professor/classes")
      .then(setClasses)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <ProfessorShell>
      <PageHeader title="Mes classes" subtitle="Les classes qui suivent vos matières, avec leurs étudiants inscrits." />
      <ErrorBox message={error} />
      {loading && <SectionLoader />}
      {!loading && classes.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune classe pour le moment : elles apparaissent quand une de vos matières est rattachée à une classe.
        </p>
      )}
      <div className="mt-6 space-y-6">
        {classes.map((c) => (
          <section key={c.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">{c.name}</h2>
                <p className="text-sm text-slate-500">{c.level ?? "-"} · {c.academicYear}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {c.subjects.map((s) => (
                  <span key={s.id} className="rounded-md bg-emerald-100 px-2 py-1 text-xs font-bold text-emerald-700">
                    {s.code} · {s.name}
                  </span>
                ))}
              </div>
            </div>
            <div className={tableWrap}>
              <table className="w-full text-left text-sm">
                <thead className={theadCls}>
                  <tr>
                    <th className="px-5 py-3">Étudiant</th>
                    <th className="px-5 py-3">N° étudiant</th>
                    <th className="px-5 py-3">Groupe</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {c.students.length === 0 && (
                    <tr><td colSpan={3} className="px-5 py-6 text-center text-slate-500">Aucun étudiant inscrit.</td></tr>
                  )}
                  {c.students.map((s) => (
                    <tr key={s.id}>
                      <td className="px-5 py-3 font-medium text-slate-900">{s.name}</td>
                      <td className="px-5 py-3 text-slate-600">{s.studentNumber}</td>
                      <td className="px-5 py-3 text-slate-600">{s.section ?? "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ))}
      </div>
    </ProfessorShell>
  );
}