"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <ProfessorShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl">
        <EmailPreferenceCard />
      </div>
    </ProfessorShell>
  );
}