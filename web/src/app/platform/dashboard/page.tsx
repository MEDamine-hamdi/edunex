"use client";

import { useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { BarList, Panel, StatCard } from "@/components/Stats";
import { ErrorBox, PageHeader, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { roleLabels } from "@/lib/roles";

type PlatformStats = {
  totalUsers: number;
  usersByRole: { role: string; count: number }[];
  usersByStatus: { status: string; count: number }[];
  logEntries: number;
  settings: number;
  last7Days: { date: string; logins: number; failedLogins: number; changes: number }[];
};

const STATUS: Record<string, string> = { ACTIVE: "Actifs", INACTIVE: "Inactifs", SUSPENDED: "Suspendus" };

export default function PlatformDashboardPage() {
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<PlatformStats>("/api/platform/stats")
      .then(setStats)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      });
  }, []);

  return (
    <PlatformShell>
      <PageHeader title="Tableau de bord" subtitle="Vue d'ensemble de la plateforme." />
      <ErrorBox message={error} />
      {!stats && !error && <p className="mt-6 text-slate-500">Chargement...</p>}

      {stats && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <StatCard label="Utilisateurs" value={stats.totalUsers} tone="blue" />
            <StatCard label="Entrées du journal" value={stats.logEntries} />
            <StatCard label="Paramètres" value={stats.settings} />
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <Panel title="Utilisateurs par rôle">
              <BarList items={stats.usersByRole.map((r) => ({ label: roleLabels[r.role] ?? r.role, value: r.count }))} />
            </Panel>
            <Panel title="Comptes par statut">
              <BarList
                items={stats.usersByStatus.map((s) => ({ label: STATUS[s.status] ?? s.status, value: s.count }))}
                color="bg-emerald-500"
              />
            </Panel>
          </div>

          <h2 className="mt-8 text-lg font-semibold">Activité des 7 derniers jours</h2>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr>
                  <th className="px-5 py-3">Jour</th>
                  <th className="px-5 py-3">Connexions</th>
                  <th className="px-5 py-3">Échecs de connexion</th>
                  <th className="px-5 py-3">Modifications</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {stats.last7Days.map((d) => (
                  <tr key={d.date}>
                    <td className="px-5 py-3 text-slate-700">
                      {new Date(d.date + "T00:00:00").toLocaleDateString("fr-FR", { weekday: "short", day: "2-digit", month: "short" })}
                    </td>
                    <td className="px-5 py-3 text-slate-700">{d.logins}</td>
                    <td className={`px-5 py-3 ${d.failedLogins > 0 ? "font-semibold text-red-600" : "text-slate-700"}`}>{d.failedLogins}</td>
                    <td className="px-5 py-3 text-slate-700">{d.changes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </PlatformShell>
  );
}