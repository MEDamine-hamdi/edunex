"use client";

import { useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Course, SubjectRef } from "@/lib/types";

export default function ProfessorSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiFetch<SubjectRef[]>("/api/professor/subjects"),
      apiFetch<Course[]>("/api/professor/courses"),
    ])
      .then(([s, c]) => {
        setSubjects(s);
        setCourses(c);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <ProfessorShell>
      <PageHeader title="Mes matières" subtitle="Les matières qui vous sont affectées par l'administration." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && subjects.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune matière ne vous est affectée pour le moment. Contactez l&apos;administration.
        </p>
      )}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {subjects.map((s) => {
          const count = courses.filter((c) => c.subject.id === s.id).length;
          return (
            <div key={s.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <span className="rounded-md bg-emerald-100 px-2 py-1 text-xs font-bold text-emerald-700">{s.code}</span>
              <h2 className="mt-3 text-lg font-semibold">{s.name}</h2>
              <p className="mt-1 text-sm text-slate-500">
                {count} cours publié{count > 1 ? "s" : ""}
              </p>
            </div>
          );
        })}
      </div>
    </ProfessorShell>
  );
}