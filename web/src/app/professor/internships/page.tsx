"use client";

import { useEffect, useState } from "react";
import { DefenseDialog } from "@/components/DefenseDialog";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { DefenseRow, InternshipRow, ROLE_LABEL, dateOnly, dateTime, resultBadge } from "@/lib/internships";

export default function ProfessorInternshipsPage() {
  const [interns, setInterns] = useState<InternshipRow[]>([]);
  const [defenses, setDefenses] = useState<DefenseRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([apiFetch<InternshipRow[]>("/api/internships/mine"), apiFetch<DefenseRow[]>("/api/defenses/mine")])
      .then(([i, d]) => {
        setInterns(i);
        setDefenses(d);
      })
      .catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <ProfessorShell>
      <PageHeader title="Stages et soutenances" subtitle="Les stagiaires que vous encadrez et les soutenances où vous êtes dans le jury." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}

      <h2 className="mt-8 text-lg font-semibold">Mes stagiaires</h2>
      {!loading && interns.length === 0 && <p className="mt-3 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Vous n&apos;encadrez aucun stage pour le moment.</p>}
      <div className="mt-3 grid gap-4 md:grid-cols-2">
        {interns.map((i) => (
          <article key={i.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="font-semibold text-slate-900">{i.student.name}</p>
            <a href={`mailto:${i.student.email}`} className="text-xs text-blue-600 hover:underline">{i.student.email}</a>
            <p className="mt-2 text-sm text-slate-700">{i.title}</p>
            <p className="text-xs text-slate-500">{[i.kind, i.company].filter(Boolean).join(" · ")} · {dateOnly(i.startDate)} - {dateOnly(i.endDate)}</p>
            {i.defenseId && <button onClick={() => setOpenId(i.defenseId)} className="mt-3 text-sm font-semibold text-blue-600 hover:underline">Voir la soutenance</button>}
          </article>
        ))}
      </div>

      <h2 className="mt-10 text-lg font-semibold">Soutenances</h2>
      {!loading && defenses.length === 0 && <p className="mt-3 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune soutenance.</p>}
      <div className="mt-3 space-y-3">
        {defenses.map((d) => {
          const badge = resultBadge(d.grade, d.status);
          return (
            <button key={d.id} onClick={() => setOpenId(d.id)} className="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 text-start shadow-sm transition hover:border-blue-300">
              <div className="min-w-0">
                <p className="font-semibold text-slate-900">{d.student.name}</p>
                <p className="truncate text-sm text-slate-600">{d.title}</p>
                <p className="mt-1 text-xs text-slate-500">{dateTime(d.scheduledAt)} · Salle {d.room ?? "-"} · Jury : {d.jury.map((j) => `${ROLE_LABEL[j.role]} ${j.name}`).join(", ")}</p>
              </div>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${badge.cls}`}>{badge.text}</span>
            </button>
          );
        })}
      </div>
      {openId && <DefenseDialog id={openId} canEdit={false} onClose={() => setOpenId(null)} />}
    </ProfessorShell>
  );
}