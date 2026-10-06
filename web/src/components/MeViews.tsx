"use client";

import { roleLabels } from "@/lib/roles";
import { useMe } from "./RoleShell";

export function WelcomePanel({ upcoming }: { upcoming: string[] }) {
  const me = useMe();
  return (
    <>
      <h1 className="text-2xl font-extrabold tracking-tight">Bonjour {me.firstName}</h1>
      <p className="mt-1 text-sm text-slate-500">Bienvenue sur EduNex.</p>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Mon compte</h2>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">Nom</dt>
              <dd className="font-medium">{me.firstName} {me.lastName}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">Email</dt>
              <dd className="font-medium">{me.email}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-slate-500">Rôle</dt>
              <dd className="font-medium">{roleLabels[me.role ?? ""] ?? me.role}</dd>
            </div>
          </dl>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Prochainement</h2>
          <p className="mt-1 text-sm text-slate-500">Ces fonctionnalités arrivent avec les prochains modules.</p>
          <ul className="mt-4 space-y-2 text-sm text-slate-700">
            {upcoming.map((item) => (
              <li key={item} className="flex items-center gap-3 rounded-xl bg-slate-50 px-4 py-3">
                <span className="h-2 w-2 rounded-full bg-blue-400" />
                {item}
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}

export function ProfilePanel() {
  const me = useMe();
  const rows: [string, string][] = [
    ["Prénom", me.firstName],
    ["Nom", me.lastName],
    ["Email", me.email],
    ["Téléphone", me.phone ?? "-"],
    ["Rôle", roleLabels[me.role ?? ""] ?? me.role ?? "-"],
    ["Statut", me.status === "ACTIVE" ? "Actif" : me.status],
  ];
  return (
    <>
      <h1 className="text-2xl font-extrabold tracking-tight">Mon profil</h1>
      <p className="mt-1 text-sm text-slate-500">
        La modification du profil sera disponible prochainement.
      </p>
      <section className="mt-6 max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <dl className="divide-y divide-slate-100 text-sm">
          {rows.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 py-3">
              <dt className="text-slate-500">{label}</dt>
              <dd className="font-medium text-slate-900">{value}</dd>
            </div>
          ))}
        </dl>
      </section>
    </>
  );
}