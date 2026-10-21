"use client";

import { DocumentRequestsPanel } from "@/components/DocumentRequestsPanel";
import { StudentShell } from "@/components/StudentShell";

export default function StudentRequestsPage() {
  return (
    <StudentShell>
      <DocumentRequestsPanel />
    </StudentShell>
  );
}