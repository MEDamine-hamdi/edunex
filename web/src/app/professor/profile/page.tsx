"use client";

import { ProfessorShell } from "@/components/ProfessorShell";
import { EmailChangeCard } from "@/components/EmailChangeCard";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <ProfessorShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl space-y-6">
        <EmailChangeCard />
        <EmailPreferenceCard />
      </div>
    </ProfessorShell>
  );
}