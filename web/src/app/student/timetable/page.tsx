"use client";

import { StudentShell } from "@/components/StudentShell";
import { PageHeader } from "@/components/ui";
import { WeekCalendar } from "@/components/WeekCalendar";

export default function StudentTimetablePage() {
  return (
    <StudentShell>
      <PageHeader title="Mon emploi du temps" subtitle="Les séances de votre classe. Une séance annulée apparaît en rouge." />
      <WeekCalendar endpoint="/api/student/timetable/week" mode="student" showProfessor />
    </StudentShell>
  );
}