"use client";

import { PlatformShell } from "@/components/PlatformShell";
import { EmailPreferenceCard } from "@/components/EmailPreferenceCard";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <PlatformShell>
      <ProfileEditor />
      <div className="mt-6 max-w-5xl">
        <EmailPreferenceCard />
      </div>
    </PlatformShell>
  );
}