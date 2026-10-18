"use client";

import { AdminShell } from "@/components/AdminShell";
import { NotificationsPanel } from "@/components/NotificationsPanel";

export default function NotificationsPage() {
  return (
    <AdminShell>
      <NotificationsPanel />
    </AdminShell>
  );
}