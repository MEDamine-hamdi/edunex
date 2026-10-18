"use client";

import { PlatformShell } from "@/components/PlatformShell";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <PlatformShell>
      <ProfileEditor />
    </PlatformShell>
  );
}