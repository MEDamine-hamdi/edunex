"use client";

import { StudentShell } from "@/components/StudentShell";
import { ProfilePanel } from "@/components/MeViews";

export default function StudentProfile() {
  return (
    <StudentShell>
      <ProfilePanel />
    </StudentShell>
  );
}