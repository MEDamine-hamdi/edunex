"use client";

import { MessagingApp } from "@/components/MessagingApp";
import { ProfessorShell } from "@/components/ProfessorShell";

export default function MessagesPage() {
  return (
    <ProfessorShell>
      <MessagingApp />
    </ProfessorShell>
  );
}