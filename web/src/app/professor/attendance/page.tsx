"use client";

import { useCallback, useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { SubjectRef } from "@/lib/types";

type Status = "PRESENT" | "ABSENT" | "LATE";
type RosterEntry = { studentId: number; studentName: string; studentNumber: string; className: string };
type AttRecord = { studentId: number; status: Status; justification: string | null };
type Summary = {
  studentId: number;
  studentName: string;
  studentNumber: string;
  present: number;
  absent: number;
  late: number;
  total: number;
};
type Mark = { status: Status; justification: string };

const STATUS: Record<Status, { label: string; on: string }> = {
  PRESENT: { label: "Présent", on: "bg-emerald-600 text-white" },
  ABSENT: { label: "Absent", on: "bg-red-600 text-white" },
  LATE: { label: "Retard", on: "bg-orange-500 text-white" },
};

function todayStr() {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export default function ProfessorAttendancePage() {
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [subjectId, setSubjectId] = useState("");
  const [date, setDate] = useState(todayStr());
  const [roster, setRoster] = useState<RosterEntry[]>([]);
  const [marks, setMarks] = useState<Record<number, Mark>>({});
  const [summary, setSummary] = useState<Summary[]>([]);
  const [loading, setLoading] = useState(true);
  const [rosterLoading, setRosterLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<SubjectRef[]>("/api/professor/subjects")
      .then((s) => {
        setSubjects(s);
        if (s[0]) setSubjectId(String(s[0].id));
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      })
      .finally(() => setLoading(false));
  }, []);

  const loadDay = useCallback(async (sid: string, d: string) => {
    setRosterLoading(true);
    setError(null);
    setSaved(false);
    try {
      const [r, recs, sum] = await Promise.all([
        apiFetch<RosterEntry[]>(`/api/professor/subjects/${sid}/students`),
        apiFetch<AttRecord[]>(`/api/professor/subjects/${sid}/attendance?date=${d}`),
        apiFetch<Summary[]>(`/api/professor/subjects/${sid}/attendance/summary`),
      ]);
      const byStudent = new Map(recs.map((x) => [x.studentId, x]));
      const initial: Record<number, Mark> = {};
      r.forEach((st) => {
        const ex = byStudent.get(st.studentId);
        initial[st.studentId] = { status: ex?.status ?? "PRESENT", justification: ex?.justification ?? "" };
      });
      setRoster(r);
      setMarks(initial);
      setSummary(sum);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setRosterLoading(false);
    }
  }, []);

  useEffect(() => {
    if (subjectId && date) loadDay(subjectId, date);
  }, [subjectId, date, loadDay]);

  const setStatus = (id: number, status: Status) => {
    setSaved(false);
    setMarks((prev) => ({ ...prev, [id]: { ...prev[id], status } }));
  };
  const setJustification = (id: number, justification: string) => {
    setSaved(false);
    setMarks((prev) => ({ ...prev, [id]: { ...prev[id], justification } }));
  };
  const allPresent = () => {
    setSaved(false);
    setMarks((prev) =>
      Object.fromEntries(Object.entries(prev).map(([k, v]) => [k, { ...v, status: "PRESENT" as Status }])) as Record<number, Mark>
    );
  };

  async function save() {
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      await apiFetch<unknown>(`/api/professor/subjects/${subjectId}/attendance`, {
        method: "PUT",
        body: JSON.stringify({
          date,
          records: roster.map((st) => {
            const m = marks[st.studentId];
            return {
              studentId: st.studentId,
              status: m.status,
              justification: m.status === "PRESENT" ? null : m.justification.trim() || null,
            };
          }),
        }),
      });
      setSaved(true);
      setSummary(await apiFetch<Summary[]>(`/api/professor/subjects/${subjectId}/attendance/summary`));
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.status === 400 && e.message.includes("future") ? "La date ne peut pas être dans le futur." : e.message);
      }
    } finally {
      setSaving(false);
    }
  }

  const counts = Object.values(marks).reduce(
    (acc, m) => ({ ...acc, [m.status]: acc[m.status] + 1 }),
    { PRESENT: 0, ABSENT: 0, LATE: 0 } as Record<Status, number>
  );

  return (
    <ProfessorShell>
      <PageHeader title="Présences" subtitle="Faites l'appel pour vos matières : présent, absent ou en retard." />
      <ErrorBox message={error} />

      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && subjects.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucune matière ne vous est affectée pour le moment. Contactez l&apos;administration.
        </p>
      )}

      {subjects.length > 0 && (
        <>
          <div className="mt-6 flex flex-wrap items-end gap-4">
            <label className="text-sm font-medium text-slate-700">
              Matière
              <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className={`${inputCls} w-72`}>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                ))}
              </select>
            </label>
            <label className="text-sm font-medium text-slate-700">
              Date
              <input
                type="date"
                value={date}
                max={todayStr()}
                onChange={(e) => e.target.value && setDate(e.target.value)}
                className={`${inputCls} w-48`}
              />
            </label>
            <button onClick={allPresent} disabled={roster.length === 0} className={smallBtn}>
              Tout présent
            </button>
          </div>

          {rosterLoading && <p className="mt-6 text-slate-500">Chargement de la liste...</p>}
          {!rosterLoading && roster.length === 0 && (
            <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
              Aucun étudiant inscrit dans une classe qui suit cette matière.
            </p>
          )}

          {!rosterLoading && roster.length > 0 && (
            <>
              <div className={tableWrap}>
                <table className="w-full text-left text-sm">
                  <thead className={theadCls}>
                    <tr>
                      <th className="px-5 py-3">Étudiant</th>
                      <th className="px-5 py-3">Classe</th>
                      <th className="px-5 py-3">Statut</th>
                      <th className="px-5 py-3">Justification</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {roster.map((st) => {
                      const m = marks[st.studentId];
                      if (!m) return null;
                      return (
                        <tr key={st.studentId}>
                          <td className="px-5 py-4">
                            <span className="font-medium text-slate-900">{st.studentName}</span>
                            <span className="block text-xs text-slate-500">{st.studentNumber}</span>
                          </td>
                          <td className="px-5 py-4 text-slate-600">{st.className}</td>
                          <td className="px-5 py-4">
                            <div className="flex gap-1.5">
                              {(Object.keys(STATUS) as Status[]).map((s) => (
                                <button
                                  key={s}
                                  onClick={() => setStatus(st.studentId, s)}
                                  className={`rounded-lg px-3 py-1.5 text-xs font-medium ${
                                    m.status === s ? STATUS[s].on : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                                  }`}
                                >
                                  {STATUS[s].label}
                                </button>
                              ))}
                            </div>
                          </td>
                          <td className="px-5 py-4">
                            {m.status !== "PRESENT" ? (
                              <input
                                value={m.justification}
                                maxLength={500}
                                onChange={(e) => setJustification(st.studentId, e.target.value)}
                                placeholder="Justification (optionnel)"
                                className={`${inputCls} mt-0 w-full min-w-48`}
                              />
                            ) : (
                              <span className="text-slate-400">-</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="mt-4 flex flex-wrap items-center gap-4">
                <button onClick={save} disabled={saving} className={primaryBtn}>
                  {saving ? "Enregistrement..." : "Enregistrer l'appel"}
                </button>
                <p className="text-sm text-slate-600">
                  {counts.PRESENT} présent(s) · {counts.ABSENT} absent(s) · {counts.LATE} en retard
                </p>
                {saved && <p className="text-sm font-medium text-emerald-700">Appel enregistré.</p>}
              </div>

              <h2 className="mt-10 text-lg font-semibold">Récapitulatif de la matière</h2>
              <div className={tableWrap}>
                <table className="w-full text-left text-sm">
                  <thead className={theadCls}>
                    <tr>
                      <th className="px-5 py-3">Étudiant</th>
                      <th className="px-5 py-3">Présences</th>
                      <th className="px-5 py-3">Absences</th>
                      <th className="px-5 py-3">Retards</th>
                      <th className="px-5 py-3">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {summary.map((s) => (
                      <tr key={s.studentId}>
                        <td className="px-5 py-3 font-medium text-slate-900">{s.studentName}</td>
                        <td className="px-5 py-3 text-slate-600">{s.present}</td>
                        <td className="px-5 py-3 text-slate-600">{s.absent}</td>
                        <td className="px-5 py-3 text-slate-600">{s.late}</td>
                        <td className="px-5 py-3 text-slate-600">{s.total}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </>
      )}
    </ProfessorShell>
  );
}