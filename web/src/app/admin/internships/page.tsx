"use client";

import { AdminShell } from "@/components/AdminShell";
import { InternshipsAdmin } from "@/components/InternshipsAdmin";

export default function AdminInternshipsPage() {
  return (
    <AdminShell>
      <InternshipsAdmin />
    </AdminShell>
  );
}