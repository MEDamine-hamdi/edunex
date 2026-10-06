"use client";

import { AdminShell } from "@/components/AdminShell";
import { AccountsManager, Status } from "@/components/AccountsManager";

type Professor = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  employeeNumber: string;
  department: string | null;
  specialty: string | null;
  status: Status;
};

export default function ProfessorsPage() {
  return (
    <AdminShell>
      <AccountsManager<Professor>
        title="Professeurs"
        subtitle="Créez et gérez les comptes des professeurs."
        endpoint="/api/admin/professors"
        newLabel="+ Nouveau professeur"
        fields={[
          { key: "firstName", label: "Prénom", required: true },
          { key: "lastName", label: "Nom", required: true },
          { key: "email", label: "Email", type: "email", required: true },
          { key: "password", label: "Mot de passe (8 caractères min.)", type: "password", required: true },
          { key: "phone", label: "Téléphone" },
          { key: "employeeNumber", label: "Numéro d'employé", required: true },
          { key: "department", label: "Département" },
          { key: "specialty", label: "Spécialité" },
        ]}
        columns={[
          { label: "Nom", render: (p) => <span className="font-medium text-slate-900">{p.firstName} {p.lastName}</span> },
          { label: "Email", render: (p) => p.email },
          { label: "N° employé", render: (p) => p.employeeNumber },
          { label: "Département", render: (p) => p.department ?? "-" },
          { label: "Spécialité", render: (p) => p.specialty ?? "-" },
        ]}
      />
    </AdminShell>
  );
}