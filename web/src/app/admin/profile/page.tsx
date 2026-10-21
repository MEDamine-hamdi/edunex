"use client";

import { AdminShell } from "@/components/AdminShell";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <AdminShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl">
        <EmailPreferenceCard />
      </div>
    </AdminShell>
  );
}