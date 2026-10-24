"use client";

import { PlatformShell } from "@/components/PlatformShell";
import { EmailChangeCard } from "@/components/EmailChangeCard";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <PlatformShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl space-y-6">
        <EmailChangeCard />
        <EmailPreferenceCard />
      </div>
    </PlatformShell>
  );
}