"use client";

import { SectionLoader } from "@/components/Loading";
import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, inputCls, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Status = "PRESENT" | "ABSENT" | "LATE";
type Stat = {
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  present: number;
  absent: number;
  late: number;
  total: number;
  absenceRate: number;
};
type Rec = {
  id: number;
  subjectId: number;
  subjectName: string;
  date: string;
  status: Status;
  justification: string | null;
};

const BADGE: Record<Status, { label: string; cls: string }> = {
  PRESENT: { label: "Présent", cls: "bg-emerald-100 text-emerald-700" },
  ABSENT: { label: "Absent", cls: "bg-red-100 text-red-700" },
  LATE: { label: "En retard", cls: "bg-orange-100 text-orange-700" },
};

const fmt = (d: string) =>
  new Date(d + "T00:00:00").toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });

export default function StudentAttendancePage() {
  const [stats, setStats] = useState<Stat[]>([]);
  const [records, setRecords] = useState<Rec[]>([]);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiFetch<Stat[]>("/api/student/attendance/stats"),
      apiFetch<Rec[]>("/api/student/attendance"),
    ])
      .then(([s, r]) => {
        setStats(s);
        setRecords(r);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  const visible = filter ? records.filter((r) => String(r.subjectId) === filter) : records;

  return (
    <StudentShell>
      <PageHeader title="Mes absences" subtitle="Vos présences, absences et retards par matière." />
      <ErrorBox message={error} />
      {loading && <SectionLoader />}
      {!loading && stats.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune donnée pour le moment. Vos présences apparaîtront ici une fois que vos professeurs auront fait l&apos;appel.
        </p>
      )}

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((s) => (
          <section key={s.subjectId} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <span className="rounded-md bg-sky-100 px-2 py-1 text-xs font-bold text-sky-700">{s.subjectCode}</span>
            <h2 className="mt-3 text-lg font-semibold">{s.subjectName}</h2>
            <dl className="mt-4 grid grid-cols-3 gap-2 text-center text-sm">
              <div className="rounded-xl bg-emerald-50 py-2">
                <dd className="text-xl font-bold text-emerald-700">{s.present}</dd>
                <dt className="text-xs text-slate-500">Présences</dt>
              </div>
              <div className="rounded-xl bg-red-50 py-2">
                <dd className="text-xl font-bold text-red-700">{s.absent}</dd>
                <dt className="text-xs text-slate-500">Absences</dt>
              </div>
              <div className="rounded-xl bg-orange-50 py-2">
                <dd className="text-xl font-bold text-orange-600">{s.late}</dd>
                <dt className="text-xs text-slate-500">Retards</dt>
              </div>
            </dl>
            <p className="mt-4 text-sm text-slate-600">
              {s.total === 0
                ? "Aucun appel enregistré."
                : `Taux d'absence : ${s.absenceRate} % (${s.total} séance${s.total > 1 ? "s" : ""})`}
            </p>
          </section>
        ))}
      </div>

      {records.length > 0 && (
        <>
          <div className="mt-10 flex flex-wrap items-center justify-between gap-4">
            <h2 className="text-lg font-semibold">Historique</h2>
            {stats.length > 1 && (
              <select value={filter} onChange={(e) => setFilter(e.target.value)} className={`${inputCls} mt-0 w-64`}>
                <option value="">Toutes les matières</option>
                {stats.map((s) => (
                  <option key={s.subjectId} value={s.subjectId}>{s.subjectName}</option>
                ))}
              </select>
            )}
          </div>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr>
                  <th className="px-5 py-3">Date</th>
                  <th className="px-5 py-3">Matière</th>
                  <th className="px-5 py-3">Statut</th>
                  <th className="px-5 py-3">Justification</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visible.map((r) => (
                  <tr key={r.id}>
                    <td className="px-5 py-3 text-slate-700">{fmt(r.date)}</td>
                    <td className="px-5 py-3 text-slate-700">{r.subjectName}</td>
                    <td className="px-5 py-3">
                      <span className={`rounded-full px-3 py-1 text-xs font-semibold ${BADGE[r.status].cls}`}>
                        {BADGE[r.status].label}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-600">{r.justification ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </StudentShell>
  );
}