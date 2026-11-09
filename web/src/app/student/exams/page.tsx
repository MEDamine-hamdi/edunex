"use client";

import { useEffect, useState } from "react";
import { ExamTimetable, PlanItem } from "@/components/ExamTimetable";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Row = { id: number; examType: string; subjectName: string; subjectCode: string; date: string; startTime: string; endTime: string; room: string | null; status: string };

export default function StudentExamsPage() {
  const [items, setItems] = useState<PlanItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Row[]>("/api/student/exams")
      .then((rows) => setItems(rows.map((r) => ({ key: String(r.id), date: r.date, start: r.startTime, end: r.endTime, title: `${r.subjectName} (${r.examType})`, detail: r.subjectCode, room: r.room, cancelled: r.status === "CANCELLED" }))))
      .catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.message));
  }, []);

  return (
    <StudentShell>
      <PageHeader title="Mes examens" subtitle="Le calendrier de vos examens : date, heure et salle." />
      <ErrorBox message={error} />
      <ExamTimetable items={items} empty="Aucun examen à venir." />
    </StudentShell>
  );
}