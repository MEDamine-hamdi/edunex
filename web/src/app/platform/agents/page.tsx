"use client";

import { PlatformShell } from "@/components/PlatformShell";
import { AccountsManager, Status } from "@/components/AccountsManager";
import { CinCell } from "@/components/CinCell";

type Agent = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  employeeNumber: string;
  service: string | null;
  cin: string | null;
  status: Status;
};

export default function AgentsPage() {
  return (
    <PlatformShell>
      <AccountsManager<Agent>
        title="Agents d'administration"
        subtitle="Créez et gérez les comptes des agents de l'administration universitaire."
        endpoint="/api/platform/agents"
        newLabel="+ Nouvel agent"
        fields={[
          { key: "firstName", label: "Prénom", required: true },
          { key: "lastName", label: "Nom", required: true },
          { key: "email", label: "Email", type: "email", required: true },
          { key: "cin", label: "CIN (8 chiffres)", required: true },
          { key: "phone", label: "Téléphone" },
          { key: "employeeNumber", label: "Numéro d'employé", required: true },
          { key: "service", label: "Service" },
        ]}
        columns={[
          { label: "Nom", render: (a) => <span className="font-medium text-slate-900">{a.firstName} {a.lastName}</span> },
          { label: "Email", render: (a) => a.email },
          { label: "CIN", render: (a) => <CinCell userId={a.id} value={a.cin} /> },
          { label: "N° employé", render: (a) => a.employeeNumber },
          { label: "Service", render: (a) => a.service ?? "-" },
        ]}
      />
    </PlatformShell>
  );
}