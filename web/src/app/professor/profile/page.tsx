"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { ProfilePanel } from "@/components/MeViews";

export default function ProfessorProfile() {
  return (
    <ProfessorShell>
      <ProfilePanel />
    </ProfessorShell>
  );
}