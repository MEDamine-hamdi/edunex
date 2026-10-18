"use client";

import { StudentShell } from "@/components/StudentShell";
import { NotificationsPanel } from "@/components/NotificationsPanel";

export default function NotificationsPage() {
  return (
    <StudentShell>
      <NotificationsPanel />
    </StudentShell>
  );
}