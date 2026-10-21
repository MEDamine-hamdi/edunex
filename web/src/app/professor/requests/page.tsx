"use client";

import { DocumentRequestsPanel } from "@/components/DocumentRequestsPanel";
import { ProfessorShell } from "@/components/ProfessorShell";

export default function ProfessorRequestsPage() {
  return (
    <ProfessorShell>
      <DocumentRequestsPanel />
    </ProfessorShell>
  );
}