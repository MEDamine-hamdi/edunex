"use client";

import { useEffect, useState } from "react";
import { DefenseDialog } from "@/components/DefenseDialog";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, primaryBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { InternshipRow, dateOnly, dateTime } from "@/lib/internships";

export default function StudentInternshipPage() {
  const [items, setItems] = useState<InternshipRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);

  useEffect(() => {
    apiFetch<InternshipRow[]>("/api/internships/mine")
      .then(setItems)
      .catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <StudentShell>
      <PageHeader title="Mon stage" subtitle="Votre stage, votre encadrant et votre soutenance." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && items.length === 0 && <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun stage enregistré pour le moment.</p>}
      <div className="mt-6 space-y-4">
        {items.map((i) => (
          <article key={i.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">{i.title}</h2>
                <p className="text-sm text-slate-500">{[i.kind, i.company].filter(Boolean).join(" · ")}</p>
              </div>
              {i.defenseId && <button onClick={() => setOpenId(i.defenseId)} className={primaryBtn}>Voir la soutenance</button>}
            </div>
            <dl className="mt-4 grid gap-x-6 gap-y-3 text-sm sm:grid-cols-3">
              <div><dt className="text-xs text-slate-400">Encadrant</dt><dd className="font-medium text-slate-800">{i.supervisor.name}</dd><a href={`mailto:${i.supervisor.email}`} className="text-xs text-blue-600 hover:underline">{i.supervisor.email}</a></div>
              <div><dt className="text-xs text-slate-400">Période</dt><dd className="text-slate-800">{dateOnly(i.startDate)} - {dateOnly(i.endDate)}</dd></div>
              <div><dt className="text-xs text-slate-400">Soutenance</dt><dd className="text-slate-800">{i.defenseAt ? dateTime(i.defenseAt) : "Pas encore planifiée"}</dd></div>
            </dl>
          </article>
        ))}
      </div>
      {openId && <DefenseDialog id={openId} canEdit={false} onClose={() => setOpenId(null)} />}
    </StudentShell>
  );
}