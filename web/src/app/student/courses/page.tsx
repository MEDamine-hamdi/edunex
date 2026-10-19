"use client";

import { useEffect, useState } from "react";
import { AssistantChat, ChatScope } from "@/components/AssistantChat";
import { DocumentRow } from "@/components/CourseParts";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Course, SubjectRef } from "@/lib/types";

export default function StudentCoursesPage() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [filter, setFilter] = useState("");
  const [chat, setChat] = useState<ChatScope | null>(null);
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
  const activeSubject = filter
    ? subjects.find((s) => String(s.id) === filter)
    : subjects.length === 1
      ? subjects[0]
      : undefined;

  return (
    <StudentShell>
      <PageHeader
        title="Mes cours"
        subtitle="Les cours et documents de vos matières. L'assistant s'appuie sur les documents déposés par vos professeurs."
        action={
          <div className="flex flex-wrap items-center gap-3">
            {subjects.length > 1 && (
              <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${inputCls} mt-0 w-64`}>
                <option value="">Toutes les matières</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            )}
            {activeSubject && (
              <button
                onClick={() => setChat({ subjectId: activeSubject.id, label: activeSubject.name })}
                className={primaryBtn}
              >
                💬 Assistant de la matière
              </button>
            )}
          </div>
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
            <div className="flex flex-wrap items-start justify-between gap-3">
              <span className="rounded-md bg-sky-100 px-2 py-1 text-xs font-bold text-sky-700">
                {c.subject.code} · {c.subject.name}
              </span>
              <button
                onClick={() => setChat({ subjectId: c.subject.id, courseId: c.id, label: `${c.title} · ${c.subject.name}` })}
                className={smallBtn}
              >
                💬 Poser une question sur ce cours
              </button>
            </div>
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

      {chat && (
        <div className="fixed inset-0 z-40 flex justify-end bg-slate-900/30" onClick={() => setChat(null)}>
          <aside
            className="flex h-full w-full max-w-xl flex-col bg-white shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <header className="flex items-start justify-between gap-3 border-b border-slate-200 p-4">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  {chat.courseId ? "Assistant du cours" : "Assistant de la matière"}
                </p>
                <h2 className="truncate text-lg font-semibold">{chat.label}</h2>
              </div>
              <button onClick={() => setChat(null)} className={smallBtn}>Fermer</button>
            </header>
            <div className="min-h-0 flex-1">
              <AssistantChat key={`${chat.subjectId}-${chat.courseId ?? 0}`} scope={chat} compact />
            </div>
          </aside>
        </div>
      )}
    </StudentShell>
  );
}