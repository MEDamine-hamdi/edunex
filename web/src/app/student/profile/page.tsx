"use client";

import { StudentShell } from "@/components/StudentShell";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <StudentShell>
      <ProfileEditor />
    </StudentShell>
  );
}