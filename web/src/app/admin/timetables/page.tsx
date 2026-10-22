"use client";

import { useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Tabs } from "@/components/Tabs";
import { TimetableImport } from "@/components/TimetableImport";
import { TimetableManager } from "@/components/TimetableManager";

export default function TimetablesPage() {
  const [tab, setTab] = useState("planning");
  return (
    <AdminShell>
      <Tabs
        tabs={[
          { key: "planning", label: "Planning" },
          { key: "import", label: "Importer depuis un PDF" },
        ]}
        active={tab}
        onChange={setTab}
      />
      <div className="pt-2">{tab === "planning" ? <TimetableManager /> : <TimetableImport />}</div>
    </AdminShell>
  );
}