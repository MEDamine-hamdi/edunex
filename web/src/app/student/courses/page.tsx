"use client";

import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { DocumentRow } from "@/components/CourseParts";
import { ErrorBox, PageHeader, inputCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Course, SubjectRef } from "@/lib/types";

export default function StudentCoursesPage() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiFetch<Course[]>("/api/student/courses"),
      apiFetch<SubjectRef[]>("/api/student/subjects"),
    ])
      .then(([c, s]) => {
        setCourses(c);
        setSubjects(s);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  const visible = filter ? courses.filter((c) => String(c.subject.id) === filter) : courses;

  return (
    <StudentShell>
      <PageHeader
        title="Mes cours"
        subtitle="Les cours et documents de vos matières."
        action={
          subjects.length > 1 ? (
            <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${inputCls} mt-0 w-64`}>
              <option value="">Toutes les matières</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          ) : undefined
        }
      />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && visible.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucun cours disponible pour le moment. Les cours apparaissent dès que vous êtes inscrit dans une
          classe et que vos professeurs les publient.
        </p>
      )}
      <div className="mt-6 space-y-5">
        {visible.map((c) => (
          <article key={c.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <span className="rounded-md bg-sky-100 px-2 py-1 text-xs font-bold text-sky-700">
              {c.subject.code} · {c.subject.name}
            </span>
            <h2 className="mt-3 text-lg font-semibold">{c.title}</h2>
            {c.description && <p className="mt-1 text-sm text-slate-600">{c.description}</p>}
            <p className="mt-2 text-xs text-slate-500">
              {c.professorName} · {formatDate(c.createdAt)}
            </p>
            {c.documents.length > 0 ? (
              <ul className="mt-5 space-y-2">
                {c.documents.map((d) => (
                  <DocumentRow key={d.id} doc={d} onError={setError} />
                ))}
              </ul>
            ) : (
              <p className="mt-5 text-sm text-slate-500">Aucun document pour ce cours.</p>
            )}
          </article>
        ))}
      </div>
    </StudentShell>
  );
}