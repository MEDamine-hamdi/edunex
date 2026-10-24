"use client";

import { AdminShell } from "@/components/AdminShell";
import { EmailChangeCard } from "@/components/EmailChangeCard";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <AdminShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl space-y-6">
        <EmailChangeCard />
        <EmailPreferenceCard />
      </div>
    </AdminShell>
  );
}