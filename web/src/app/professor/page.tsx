"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { WelcomePanel } from "@/components/MeViews";

export default function ProfessorHome() {
  return (
    <ProfessorShell>
      <WelcomePanel
        upcoming={[
          "Mes matières et mes classes",
          "Création de cours et dépôt de documents",
          "Appel et gestion des présences",
          "Emploi du temps",
          "Notifications",
        ]}
      />
    </ProfessorShell>
  );
}