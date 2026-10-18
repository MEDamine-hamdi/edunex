"use client";

import { AdminShell } from "@/components/AdminShell";
import { ProfileEditor } from "@/components/ProfileEditor";

export default function ProfilePage() {
  return (
    <AdminShell>
      <ProfileEditor />
    </AdminShell>
  );
}