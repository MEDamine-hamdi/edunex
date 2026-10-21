"use client";

import { useCallback, useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { SubjectRef } from "@/lib/types";

type Status = "PRESENT" | "ABSENT" | "LATE";
type SessionInfo = {
  sessionId: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  className: string;
  room: string | null;
  startTime: string;
  endTime: string;
  opensAt: string;
  closesAt: string;
};
type Current = { now: string; open: SessionInfo[]; upcoming: SessionInfo[] };
type RosterEntry = { studentId: number; studentName: string; studentNumber: string };
type AttRecord = { studentId: number; status: Status; justification: string | null };
type Summary = { studentId: number; studentName: string; present: number; absent: number; late: number; total: number };
type Mark = { status: Status; justification: string };

const STATUS: Record<Status, { label: string; on: string }> = {
  PRESENT: { label: "Présent", on: "bg-emerald-600 text-white" },
  ABSENT: { label: "Absent", on: "bg-red-600 text-white" },
  LATE: { label: "Retard", on: "bg-orange-500 text-white" },
};

export default function ProfessorAttendancePage() {
  const [current, setCurrent] = useState<Current | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [roster, setRoster] = useState<RosterEntry[]>([]);
  const [marks, setMarks] = useState<Record<number, Mark>>({});
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [subjects, setSubjects] = useState<SubjectRef[]>([]);
  const [summarySubject, setSummarySubject] = useState("");
  const [summary, setSummary] = useState<Summary[]>([]);

  const loadCurrent = useCallback(async () => {
    try {
      const c = await apiFetch<Current>("/api/professor/attendance/current");
      setCurrent(c);
      setSelected((prev) => (prev && c.open.some((s) => s.sessionId === prev) ? prev : c.open[0]?.sessionId ?? null));
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, []);

  useEffect(() => {
    loadCurrent();
    const timer = setInterval(loadCurrent, 60000);
    apiFetch<SubjectRef[]>("/api/professor/subjects")
      .then((s) => {
        setSubjects(s);
        if (s[0]) setSummarySubject(String(s[0].id));
      })
      .catch(() => {});
    return () => clearInterval(timer);
  }, [loadCurrent]);

  const session = current?.open.find((s) => s.sessionId === selected) ?? null;
  const today = current?.now.slice(0, 10) ?? "";

  useEffect(() => {
    if (!session || !today) {
      setRoster([]);
      return;
    }
    setSaved(false);
    Promise.all([
      apiFetch<RosterEntry[]>(`/api/professor/subjects/${session.subjectId}/students`),
      apiFetch<AttRecord[]>(`/api/professor/subjects/${session.subjectId}/attendance?date=${today}`),
    ])
      .then(([r, recs]) => {
        const byId = new Map(recs.map((x) => [x.studentId, x]));
        setRoster(r);
        setMarks(Object.fromEntries(r.map((st) => [st.studentId, { status: byId.get(st.studentId)?.status ?? "PRESENT", justification: byId.get(st.studentId)?.justification ?? "" }])));
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401)
          setError(e.status === 409 ? "Aucune séance en cours pour cette matière." : e.message);
        loadCurrent();
      });
  }, [session, today, loadCurrent]);

  useEffect(() => {
    if (!summarySubject) return;
    apiFetch<Summary[]>(`/api/professor/subjects/${summarySubject}/attendance/summary`).then(setSummary).catch(() => setSummary([]));
  }, [summarySubject, saved]);

  const set = (id: number, patch: Partial<Mark>) => {
    setSaved(false);
    setMarks((prev) => ({ ...prev, [id]: { ...prev[id], ...patch } }));
  };

  async function save() {
    if (!session) return;
    setSaving(true);
    setError(null);
    try {
      await apiFetch(`/api/professor/subjects/${session.subjectId}/attendance`, {
        method: "PUT",
        body: JSON.stringify({
          date: today,
          records: roster.map((st) => ({
            studentId: st.studentId,
            status: marks[st.studentId].status,
            justification: marks[st.studentId].status === "PRESENT" ? null : marks[st.studentId].justification.trim() || null,
          })),
        }),
      });
      setSaved(true);
    } catch (e) {
      if (e instanceof ApiError)
        setError(e.status === 409 ? "La séance n'est plus en cours : l'appel est fermé." : e.message);
      loadCurrent();
    } finally {
      setSaving(false);
    }
  }

  return (
    <ProfessorShell>
      <PageHeader
        title="Présences"
        subtitle="L'appel n'est possible que pendant votre séance en cours (de 10 minutes avant le début à 15 minutes après la fin)."
      />
      <ErrorBox message={error} />

      {current && current.open.length === 0 && (
        <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Aucune séance en cours.</h2>
          {current.upcoming.length > 0 ? (
            <ul className="mt-3 space-y-2 text-sm text-slate-600">
              {current.upcoming.map((s) => (
                <li key={s.sessionId} className="rounded-xl bg-slate-50 px-4 py-3">
                  Aujourd&apos;hui {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)} · {s.subjectName} · {s.className} · Salle {s.room ?? "-"}
                  <span className="block text-xs text-slate-400">L&apos;appel ouvre à {s.opensAt.slice(0, 5)}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-sm text-slate-500">Vous n&apos;avez plus de séance aujourd&apos;hui.</p>
          )}
        </section>
      )}

      {current && current.open.length > 0 && (
        <>
          <div className="mt-6 flex flex-wrap gap-3">
            {current.open.map((s) => (
              <button key={s.sessionId} onClick={() => setSelected(s.sessionId)}
                className={`rounded-2xl border px-4 py-3 text-left text-sm ${s.sessionId === selected ? "border-blue-500 bg-blue-50" : "border-slate-200 bg-white"}`}>
                <span className="block text-xs font-semibold uppercase text-emerald-600">Séance en cours</span>
                <span className="font-semibold">{s.subjectName}</span>
                <span className="block text-xs text-slate-500">{s.className} · {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)} · Salle {s.room ?? "-"}</span>
              </button>
            ))}
          </div>

          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr>
                  <th className="px-5 py-3">Étudiant</th>
                  <th className="px-5 py-3">Statut</th>
                  <th className="px-5 py-3">Justification</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {roster.length === 0 && <tr><td colSpan={3} className="px-5 py-6 text-center text-slate-500">Aucun étudiant inscrit dans cette classe.</td></tr>}
                {roster.map((st) => {
                  const m = marks[st.studentId];
                  if (!m) return null;
                  return (
                    <tr key={st.studentId}>
                      <td className="px-5 py-3">
                        <span className="font-medium text-slate-900">{st.studentName}</span>
                        <span className="block text-xs text-slate-500">{st.studentNumber}</span>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex gap-1.5">
                          {(Object.keys(STATUS) as Status[]).map((s) => (
                            <button key={s} onClick={() => set(st.studentId, { status: s })}
                              className={`rounded-lg px-3 py-1.5 text-xs font-medium ${m.status === s ? STATUS[s].on : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>
                              {STATUS[s].label}
                            </button>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-3">
                        {m.status !== "PRESENT" ? (
                          <input value={m.justification} maxLength={500} onChange={(e) => set(st.studentId, { justification: e.target.value })}
                            placeholder="Justification (optionnel)" className={`${inputCls} mt-0 min-w-48`} />
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
            <button onClick={() => { setSaved(false); setMarks((p) => Object.fromEntries(Object.entries(p).map(([k, v]) => [k, { ...v, status: "PRESENT" as Status }]))); }} className={smallBtn}>Tout présent</button>
            <button onClick={save} disabled={saving || roster.length === 0} className={primaryBtn}>{saving ? "Enregistrement..." : "Enregistrer l'appel"}</button>
            {saved && <p className="text-sm font-medium text-emerald-700">Appel enregistré.</p>}
          </div>
        </>
      )}

      {subjects.length > 0 && (
        <>
          <div className="mt-10 flex flex-wrap items-center justify-between gap-4">
            <h2 className="text-lg font-semibold">Récapitulatif de la matière</h2>
            <select value={summarySubject} onChange={(e) => setSummarySubject(e.target.value)} className={`${inputCls} mt-0 w-72`}>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
              ))}
            </select>
          </div>
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
    </ProfessorShell>
  );
}