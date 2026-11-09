"use client";

import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Row = { subjectId: number; name: string; code: string; absences: number; absent: number; late: number; max: number | null; remaining: number | null; eliminated: boolean; waived: boolean };

export default function StudentEliminationPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch<Row[]>("/api/student/elimination").then(setRows).catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.message)).finally(() => setLoading(false));
  }, []);

  return (
    <StudentShell>
      <PageHeader title="Seuil d'absences" subtitle="Le nombre d'absences autorisées dans chaque matière. Au-delà, vous êtes éliminé(e) de la matière." />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr><th className="px-5 py-3">Matière</th><th className="px-5 py-3">Absences</th><th className="px-5 py-3">Retards</th><th className="px-5 py-3">Autorisées</th><th className="px-5 py-3">Situation</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {!loading && rows.length === 0 && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucune matière.</td></tr>}
            {rows.map((r) => (
              <tr key={r.subjectId}>
                <td className="px-5 py-3 font-medium text-slate-900">{r.name}<span className="block text-xs font-normal text-slate-500">{r.code}</span></td>
                <td className="px-5 py-3 text-slate-700">{r.absences}</td>
                <td className="px-5 py-3 text-slate-700">{r.late}</td>
                <td className="px-5 py-3 text-slate-700">{r.max ?? "Pas de limite"}</td>
                <td className="px-5 py-3">
                  {r.eliminated ? <span className="rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700">Éliminé(e)</span>
                    : r.waived ? <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">Élimination levée</span>
                    : r.remaining !== null ? <span className={`rounded-full px-3 py-1 text-xs font-semibold ${r.remaining === 0 ? "bg-orange-100 text-orange-700" : "bg-emerald-100 text-emerald-700"}`}>{r.remaining === 0 ? "Limite atteinte" : `Encore ${r.remaining} absence(s) possible(s)`}</span>
                    : <span className="text-slate-400">-</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </StudentShell>
  );
}