"use client";

import { useCallback, useEffect, useState } from "react";
import { ExamTimetable, PlanItem } from "@/components/ExamTimetable";
import { ProfessorShell } from "@/components/ProfessorShell";
import { Tabs } from "@/components/Tabs";
import { ErrorBox, PageHeader, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ACTION_LABEL, EventRow, ExamItem, fullDate, hour, stamp } from "@/lib/exams";
import { ApiError, apiFetch } from "@/lib/api";

type Mine = { exam: ExamItem; mode: string; canWithdraw: boolean; past: boolean };
type Available = { exam: ExamItem; canJoin: boolean; blocked: string | null };

export default function ProfessorExamsPage() {
  const [tab, setTab] = useState("plan");
  const [kind, setKind] = useState<string | null>(null);
  const [mine, setMine] = useState<Mine[]>([]);
  const [available, setAvailable] = useState<Available[]>([]);
  const [history, setHistory] = useState<EventRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [k, m, a, h] = await Promise.all([
        apiFetch<{ kind: string }>("/api/professor/exams/kind"),
        apiFetch<Mine[]>("/api/professor/exams/mine"),
        apiFetch<Available[]>("/api/professor/exams/available"),
        apiFetch<EventRow[]>("/api/professor/exams/history"),
      ]);
      setKind(k.kind);
      setMine(m);
      setAvailable(a);
      setHistory(h);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function join(id: number) {
    setError(null);
    setInfo(null);
    try {
      await apiFetch(`/api/professor/exams/${id}/volunteer`, { method: "POST" });
      setInfo("Vous surveillerez cet examen. Il apparaît dans votre planning.");
      await load();
    } catch (e) {
      if (e instanceof ApiError) setError(e.status === 409 ? e.message : e.status === 403 ? "Seuls les vacataires choisissent leurs examens." : e.message);
    }
  }

  async function withdraw(id: number) {
    if (!window.confirm("Vous désister de cette surveillance ?")) return;
    setError(null);
    setInfo(null);
    try {
      await apiFetch<void>(`/api/professor/exams/${id}/volunteer`, { method: "DELETE" });
      await load();
    } catch (e) {
      if (e instanceof ApiError) setError(e.status === 409 ? "Trop tard pour vous désister : contactez l'administration." : e.message);
    }
  }

  const plan: PlanItem[] = mine.map((m) => ({
    key: String(m.exam.id), date: m.exam.date, start: m.exam.startTime, end: m.exam.endTime,
    title: `${m.exam.subjectName} (${m.exam.examType})`, detail: m.exam.classes.map((c) => c.name).join(", "), room: m.exam.room,
    tag: m.mode === "VOLUNTEER" ? "Surveillance choisie" : "Surveillance imposée par l'administration", cancelled: m.exam.status === "CANCELLED",
  }));
  const upcoming = mine.filter((m) => !m.past);

  return (
    <ProfessorShell>
      <PageHeader
        title="Surveillance des examens"
        subtitle={kind === "VACATAIRE" ? "Vous êtes vacataire : vous choisissez les examens que vous surveillez." : "Vous êtes permanent : l'administration fixe vos surveillances."}
      />
      <Tabs tabs={[{ key: "plan", label: "Mon planning" }, { key: "choose", label: "Examens à surveiller" }, { key: "history", label: "Historique" }]} active={tab} onChange={setTab} />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {tab === "plan" && (
        <>
          <ExamTimetable items={plan} empty="Aucune surveillance à venir." />
          {upcoming.some((m) => m.mode === "VOLUNTEER") && (
            <div className="mt-8">
              <h2 className="text-lg font-semibold">Mes surveillances choisies</h2>
              <ul className="mt-3 space-y-2">
                {upcoming.filter((m) => m.mode === "VOLUNTEER").map((m) => (
                  <li key={m.exam.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 shadow-sm">
                    <span className="text-sm text-slate-800"><span className="font-semibold">{m.exam.subjectName}</span> ({m.exam.examType}) · {fullDate(m.exam.date)} · {hour(m.exam.startTime)}-{hour(m.exam.endTime)}</span>
                    {m.canWithdraw ? <button onClick={() => withdraw(m.exam.id)} className={`${smallBtn} text-red-600`}>Se désister</button> : <span className="text-xs text-slate-500">Désistement impossible : contactez l&apos;administration</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}

      {tab === "choose" && (
        kind !== "VACATAIRE" ? (
          <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-600">En tant que professeur permanent, vous ne choisissez pas vos surveillances : l&apos;administration vous affecte aux examens (voir « Mon planning »). Vous êtes prévenu(e) par notification et par e-mail.</p>
        ) : (
          <div className="mt-6 space-y-3">
            {available.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun examen à surveiller pour le moment.</p>}
            {available.map((a) => (
              <article key={a.exam.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div>
                  <p className="font-semibold text-slate-900">{a.exam.subjectName} <span className="font-normal text-slate-500">({a.exam.examType})</span></p>
                  <p className="text-sm text-slate-600">{fullDate(a.exam.date)} · {hour(a.exam.startTime)}-{hour(a.exam.endTime)} · Salle {a.exam.room ?? "-"}</p>
                  <p className="text-xs text-slate-500">{a.exam.classes.map((c) => c.name).join(", ")} · {a.exam.supervisors.length}/{a.exam.needed} surveillants</p>
                </div>
                {a.canJoin ? <button onClick={() => join(a.exam.id)} className={primaryBtn}>Je surveille</button> : <span className="rounded-full bg-slate-200 px-3 py-1 text-xs font-semibold text-slate-600">{a.blocked}</span>}
              </article>
            ))}
          </div>
        )
      )}

      {tab === "history" && (
        <div className={tableWrap}>
          <table className="w-full text-left text-sm">
            <thead className={theadCls}><tr><th className="px-5 py-3">Date</th><th className="px-5 py-3">Action</th><th className="px-5 py-3">Examen</th><th className="px-5 py-3">Par</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {history.length === 0 && <tr><td colSpan={4} className="px-5 py-8 text-center text-slate-500">Aucun historique.</td></tr>}
              {history.map((h) => (
                <tr key={h.id}>
                  <td className="px-5 py-3 text-slate-600">{stamp(h.at)}</td>
                  <td className="px-5 py-3 font-medium text-slate-900">{ACTION_LABEL[h.action] ?? h.action}{h.reason && <span className="block text-xs font-normal text-slate-500">{h.reason}</span>}</td>
                  <td className="px-5 py-3 text-slate-700">{h.exam ?? "-"}</td>
                  <td className="px-5 py-3 text-slate-600">{h.actor ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </ProfessorShell>
  );
}