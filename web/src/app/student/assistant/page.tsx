"use client";

import Link from "next/link";
import { AssistantChat } from "@/components/AssistantChat";
import { StudentShell } from "@/components/StudentShell";
import { PageHeader, smallBtn } from "@/components/ui";

export default function StudentAssistantPage() {
  return (
    <StudentShell>
      <PageHeader
        title="Assistant de l'institut"
        subtitle="Règlement, examens, inscriptions, services, contacts : l'assistant répond à partir du guide officiel de l'institut."
        action={<Link href="/student/courses" className={smallBtn}>Questions sur un cours : Mes cours</Link>}
      />
      <div className="mt-6">
        <AssistantChat />
      </div>
    </StudentShell>
  );
}