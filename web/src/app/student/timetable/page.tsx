"use client";

import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { WeekGrid } from "@/components/WeekGrid";
import { ErrorBox, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Session } from "@/lib/types";

export default function StudentTimetablePage() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Session[]>("/api/student/timetable")
      .then(setSessions)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <StudentShell>
      <PageHeader title="Mon emploi du temps" subtitle="Les séances de votre classe." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && sessions.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune séance pour le moment. Votre emploi du temps apparaîtra dès que vous serez inscrit dans une
          classe et que l&apos;administration l&apos;aura planifié.
        </p>
      )}
      {sessions.length > 0 && <WeekGrid sessions={sessions} showProfessor />}
    </StudentShell>
  );
}