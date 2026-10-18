"use client";

import { PlatformShell } from "@/components/PlatformShell";
import { NotificationsPanel } from "@/components/NotificationsPanel";

export default function NotificationsPage() {
  return (
    <PlatformShell>
      <NotificationsPanel />
    </PlatformShell>
  );
}