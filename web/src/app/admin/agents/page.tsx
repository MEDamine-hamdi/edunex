"use client";

import { useCallback, useEffect, useState } from "react";
import { AdminAccountForm } from "@/components/AdminAccountForm";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Admin = { id: number; firstName: string; lastName: string; email: string; employeeNumber: string; service: string | null; status: string };

export default function UniversityAdminsPage() {
  const [admins, setAdmins] = useState<Admin[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setAdmins(await apiFetch<Admin[]>("/api/admin/agents"));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les comptes." : e.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function toggle(a: Admin) {
    setError(null);
    try {
      await apiFetch(`/api/admin/agents/${a.id}/status`, { method: "PUT", body: JSON.stringify({ status: a.status === "ACTIVE" ? "INACTIVE" : "ACTIVE" }) });
      await load();
    } catch (e) {
      if (e instanceof ApiError) setError(e.status === 400 ? e.message : "Action impossible.");
    }
  }

  return (
    <AdminShell>
      <PageHeader title="Administrateurs de l'université" subtitle="Les administrateurs de votre université. Les professeurs et les étudiants se créent depuis leurs pages." />
      <ErrorBox message={error} />
      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}><tr><th className="px-5 py-3">Nom</th><th className="px-5 py-3">E-mail</th><th className="px-5 py-3">N° employé</th><th className="px-5 py-3">Service</th><th className="px-5 py-3">Statut</th><th className="px-5 py-3 text-right">Action</th></tr></thead>
          <tbody className="divide-y divide-slate-100">
            {admins.map((a) => (
              <tr key={a.id}>
                <td className="px-5 py-3 font-medium text-slate-900">{a.firstName} {a.lastName}</td>
                <td className="px-5 py-3 text-slate-700">{a.email}</td>
                <td className="px-5 py-3 text-slate-700">{a.employeeNumber}</td>
                <td className="px-5 py-3 text-slate-700">{a.service ?? "-"}</td>
                <td className="px-5 py-3 text-slate-700">{a.status === "ACTIVE" ? "Actif" : "Inactif"}</td>
                <td className="px-5 py-3 text-right"><button onClick={() => toggle(a)} className={smallBtn}>{a.status === "ACTIVE" ? "Désactiver" : "Activer"}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <h2 className="mt-8 text-lg font-semibold">Ajouter un administrateur</h2>
      <div className="mt-3"><AdminAccountForm endpoint="/api/admin/agents" onCreated={load} /></div>
    </AdminShell>
  );
}