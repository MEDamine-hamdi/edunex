"use client";

import { StudentShell } from "@/components/StudentShell";
import { WelcomePanel } from "@/components/MeViews";

export default function StudentHome() {
  return (
    <StudentShell>
      <WelcomePanel
        upcoming={[
          "Mes matières, mes cours et leurs documents",
          "Emploi du temps",
          "Absences et retards",
          "Notes et moyennes",
          "Assistant EduNex AI",
          "Notifications",
        ]}
      />
    </StudentShell>
  );
}