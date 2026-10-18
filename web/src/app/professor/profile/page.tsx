"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <ProfessorShell>
      <ProfileEditor />
    </ProfessorShell>
  );
}