"use client";

import { useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { RequestTypesManager } from "@/components/RequestTypesManager";
import { RequestsInbox } from "@/components/RequestsInbox";
import { Tabs } from "@/components/Tabs";

export default function AdminRequestsPage() {
  const [tab, setTab] = useState("requests");
  return (
    <AdminShell>
      <Tabs
        tabs={[
          { key: "requests", label: "Demandes" },
          { key: "types", label: "Types de documents" },
        ]}
        active={tab}
        onChange={setTab}
      />
      <div className="pt-2">{tab === "requests" ? <RequestsInbox /> : <RequestTypesManager />}</div>
    </AdminShell>
  );
}