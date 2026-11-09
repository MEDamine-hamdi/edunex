"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Course } from "@/lib/types";

type Prof = { name: string; email: string };
type SubjectCard = { id: number; name: string; code: string; coefficient: number | null; credits: number | null; professors: Prof[] };

export default function StudentSubjectsPage() {
  const [subjects, setSubjects] = useState<SubjectCard[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiFetch<SubjectCard[]>("/api/student/subjects/details"),
      apiFetch<Course[]>("/api/student/courses"),
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
    <StudentShell>
      <PageHeader title="Mes matières" subtitle="Les matières de votre classe et leurs enseignants." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && subjects.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune matière pour le moment. Vous apparaîtrez ici dès que l&apos;administration vous aura inscrit dans une classe.
        </p>
      )}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {subjects.map((s) => {
          const count = courses.filter((c) => c.subject.id === s.id).length;
          return (
            <article key={s.id} className="flex flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <span className="w-fit rounded-md bg-sky-100 px-2 py-1 text-xs font-bold text-sky-700">{s.code}</span>
              <h2 className="mt-3 text-lg font-semibold">{s.name}</h2>
              <p className="mt-1 text-sm text-slate-500">
                {count} cours disponible{count > 1 ? "s" : ""}
                {s.credits != null ? ` · ${s.credits} crédits` : ""}
              </p>
              <div className="mt-4 space-y-2 border-t border-slate-100 pt-4">
                {s.professors.length === 0 && <p className="text-sm text-slate-400">Aucun enseignant affecté.</p>}
                {s.professors.map((p) => (
                  <p key={p.email} className="text-sm">
                    <span className="block font-medium text-slate-800">{p.name}</span>
                    <a href={`mailto:${p.email}`} className="text-xs text-blue-600 hover:underline">{p.email}</a>
                  </p>
                ))}
              </div>
              <Link href="/student/courses" className={`mt-5 w-fit ${smallBtn}`}>Voir les cours</Link>
            </article>
          );
        })}
      </div>
    </StudentShell>
  );
}