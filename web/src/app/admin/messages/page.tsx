"use client";

import { MessagingApp } from "@/components/MessagingApp";
import { AdminShell } from "@/components/AdminShell";

export default function MessagesPage() {
  return (
    <AdminShell>
      <MessagingApp />
    </AdminShell>
  );
}