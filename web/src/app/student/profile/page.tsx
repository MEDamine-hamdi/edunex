"use client";

import { StudentShell } from "@/components/StudentShell";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <StudentShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl">
        <EmailPreferenceCard />
      </div>
    </StudentShell>
  );
}