"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { PageHeader, inputCls } from "@/components/ui";
import { WeekCalendar } from "@/components/WeekCalendar";
import { apiFetch } from "@/lib/api";

type Named = { id: number; name?: string; firstName?: string; lastName?: string };

export default function AdminAbsencesPage() {
  const [classes, setClasses] = useState<Named[]>([]);
  const [professors, setProfessors] = useState<Named[]>([]);
  const [classId, setClassId] = useState("");
  const [professorId, setProfessorId] = useState("");

  useEffect(() => {
    apiFetch<Named[]>("/api/admin/classes").then(setClasses).catch(() => {});
    apiFetch<Named[]>("/api/admin/professors").then(setProfessors).catch(() => {});
  }, []);

  const query = `${classId ? `&classId=${classId}` : ""}${professorId ? `&professorId=${professorId}` : ""}`;

  return (
    <AdminShell>
      <PageHeader
        title="Absences des enseignants"
        subtitle="Signalez l'absence d'un enseignant : la séance apparaît en rouge et la classe est prévenue par notification et e-mail."
        action={
          <div className="flex flex-wrap gap-3">
            <select value={classId} onChange={(e) => setClassId(e.target.value)} className={`${inputCls} mt-0 w-56`}>
              <option value="">Toutes les classes</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
            <select value={professorId} onChange={(e) => setProfessorId(e.target.value)} className={`${inputCls} mt-0 w-56`}>
              <option value="">Tous les professeurs</option>
              {professors.map((p) => (
                <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>
              ))}
            </select>
          </div>
        }
      />
      <WeekCalendar endpoint="/api/admin/timetable/week" mode="admin" query={query} showClass showProfessor />
    </AdminShell>
  );
}