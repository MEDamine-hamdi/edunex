"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { PageHeader } from "@/components/ui";
import { WeekCalendar } from "@/components/WeekCalendar";

export default function ProfessorTimetablePage() {
  return (
    <ProfessorShell>
      <PageHeader
        title="Mon emploi du temps"
        subtitle="Vos séances. Vous pouvez déclarer une absence jusqu'à 10 heures avant le début de la séance : les étudiants sont prévenus."
      />
      <WeekCalendar endpoint="/api/professor/timetable/week" mode="professor" showClass />
    </ProfessorShell>
  );
}