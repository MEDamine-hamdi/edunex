"use client";

import { MessagingApp } from "@/components/MessagingApp";
import { StudentShell } from "@/components/StudentShell";

export default function MessagesPage() {
  return (
    <StudentShell>
      <MessagingApp />
    </StudentShell>
  );
}