"use client";

import { useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { AccountsManager, Status } from "@/components/AccountsManager";
import { CinCell } from "@/components/CinCell";
import { apiFetch } from "@/lib/api";

type Professor = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  employeeNumber: string;
  department: string | null;
  specialty: string | null;
  cin: string | null;
  professorType: string | null;
  status: Status;
};

function TypeSelect({ id, value }: { id: number; value: string | null }) {
  const [type, setType] = useState(value ?? "PERMANENT");
  const [busy, setBusy] = useState(false);

  async function change(next: string) {
    setBusy(true);
    try {
      await apiFetch(`/api/admin/professors/${id}/type`, { method: "PUT", body: JSON.stringify({ type: next }) });
      setType(next);
    } catch {
      window.alert("Enregistrement impossible.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <select
      value={type}
      disabled={busy}
      onChange={(e) => change(e.target.value)}
      className={`rounded-lg border px-2 py-1 text-xs font-semibold ${type === "VACATAIRE" ? "border-amber-300 bg-amber-50 text-amber-800" : "border-emerald-300 bg-emerald-50 text-emerald-800"}`}
    >
      <option value="PERMANENT">Permanent</option>
      <option value="VACATAIRE">Vacataire</option>
    </select>
  );
}

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
          { key: "cin", label: "CIN (8 chiffres)", required: true },
          { key: "phone", label: "Téléphone" },
          { key: "employeeNumber", label: "Numéro d'employé", required: true },
          { key: "professorType", label: "Type : Permanent ou Vacataire (vide = Permanent)" },
          { key: "department", label: "Département" },
          { key: "specialty", label: "Spécialité" },
        ]}
        columns={[
          { label: "Nom", render: (p) => <span className="font-medium text-slate-900">{p.firstName} {p.lastName}</span> },
          { label: "Email", render: (p) => p.email },
          { label: "CIN", render: (p) => <CinCell userId={p.id} value={p.cin} /> },
          { label: "Type", render: (p) => <TypeSelect id={p.id} value={p.professorType} /> },
          { label: "N° employé", render: (p) => p.employeeNumber },
          { label: "Département", render: (p) => p.department ?? "-" },
          { label: "Spécialité", render: (p) => p.specialty ?? "-" },
        ]}
      />
    </AdminShell>
  );
}