"use client";

import { AdminShell } from "@/components/AdminShell";
import { AccountsManager, Status } from "@/components/AccountsManager";

type Student = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  studentNumber: string;
  level: string | null;
  specialty: string | null;
  status: Status;
};

export default function StudentsPage() {
  return (
    <AdminShell>
      <AccountsManager<Student>
        title="Étudiants"
        subtitle="Créez et gérez les comptes des étudiants."
        endpoint="/api/admin/students"
        newLabel="+ Nouvel étudiant"
        fields={[
          { key: "firstName", label: "Prénom", required: true },
          { key: "lastName", label: "Nom", required: true },
          { key: "email", label: "Email", type: "email", required: true },
          { key: "password", label: "Mot de passe (8 caractères min.)", type: "password", required: false },
          { key: "phone", label: "Téléphone" },
          { key: "studentNumber", label: "Numéro étudiant", required: true },
          { key: "enrollmentYear", label: "Année d'inscription", type: "number" },
          { key: "level", label: "Niveau" },
          { key: "specialty", label: "Spécialité" },
        ]}
        columns={[
          { label: "Nom", render: (s) => <span className="font-medium text-slate-900">{s.firstName} {s.lastName}</span> },
          { label: "Email", render: (s) => s.email },
          { label: "N° étudiant", render: (s) => s.studentNumber },
          { label: "Niveau", render: (s) => s.level ?? "-" },
          { label: "Spécialité", render: (s) => s.specialty ?? "-" },
        ]}
      />
    </AdminShell>
  );
}