"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, inputCls, primaryBtn, smallBtn } from "./ui";
import { DAYS } from "./WeekGrid";

type Absence = { id: number; kind: "PROFESSOR" | "ADMIN"; reason: string | null; declaredBy: string };
export type WeekSession = {
  sessionId: number;
  date: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  className: string;
  subjectName: string;
  subjectCode: string;
  professorName: string;
  absence: Absence | null;
  canDeclare: boolean;
};

type Props = {
  endpoint: string;
  mode: "student" | "professor" | "admin";
  query?: string;
  showClass?: boolean;
  showProfessor?: boolean;
};

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

function mondayOf(d: Date) {
  const x = new Date(d);
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7));
  x.setHours(12, 0, 0, 0);
  return x;
}

function explain(e: unknown): string {
  if (!(e instanceof ApiError)) return "Une erreur est survenue.";
  if (e.message.includes("at least")) return "Trop tard : une absence doit être déclarée au moins 10 heures avant la séance.";
  if (e.message.includes("already marked")) return "Cette séance est déjà marquée absente.";
  if (e.message.includes("Too late")) return "Trop tard pour retirer cette absence : contactez l'administration.";
  return e.message;
}

export function WeekCalendar({ endpoint, mode, query = "", showClass, showProfessor }: Props) {
  const [week, setWeek] = useState(() => mondayOf(new Date()));
  const [sessions, setSessions] = useState<WeekSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setSessions(await apiFetch<WeekSession[]>(`${endpoint}?date=${iso(week)}${query}`));
      setError(null);
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) setError(explain(e));
    } finally {
      setLoading(false);
    }
  }, [endpoint, week, query]);

  useEffect(() => {
    load();
  }, [load]);

  const base = mode === "admin" ? "/api/admin/absences" : "/api/professor/absences";

  async function declare(s: WeekSession) {
    setBusy(true);
    setError(null);
    try {
      await apiFetch(base, { method: "POST", body: JSON.stringify({ sessionId: s.sessionId, date: s.date, reason: reason.trim() || null }) });
      setOpenId(null);
      setReason("");
      await load();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  async function withdraw(s: WeekSession) {
    if (!s.absence) return;
    setBusy(true);
    setError(null);
    try {
      await apiFetch<void>(`${base}/${s.absence.id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  const days = [...DAYS];
  if (sessions.some((s) => s.dayOfWeek === "SUNDAY")) days.push({ key: "SUNDAY", label: "Dimanche" });
  const today = iso(new Date());
  const end = new Date(week);
  end.setDate(end.getDate() + 6);
  const fmt = (d: Date) => d.toLocaleDateString(undefined, { day: "2-digit", month: "short" });
  const shift = (n: number) => setWeek((w) => { const x = new Date(w); x.setDate(x.getDate() + n * 7); return x; });

  return (
    <div>
      <div className="mt-6 flex flex-wrap items-center gap-3">
        <button onClick={() => shift(-1)} className={smallBtn}>← Semaine précédente</button>
        <button onClick={() => setWeek(mondayOf(new Date()))} className={smallBtn}>Cette semaine</button>
        <button onClick={() => shift(1)} className={smallBtn}>Semaine suivante →</button>
        <span className="text-sm font-semibold text-slate-700">{fmt(week)} - {fmt(end)}</span>
        <span className="ms-auto flex items-center gap-2 text-xs text-slate-500">
          <span className="h-3 w-3 rounded bg-red-500" /> Enseignant absent
        </span>
      </div>
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}

      <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
        {days.map((d, i) => {
          const date = new Date(week);
          date.setDate(date.getDate() + i);
          const list = sessions.filter((s) => s.dayOfWeek === d.key);
          const isToday = iso(date) === today;
          return (
            <section key={d.key} className={`rounded-2xl border bg-white p-4 shadow-sm ${isToday ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200"}`}>
              <h2 className="text-sm font-bold uppercase tracking-wide text-slate-500">
                {d.label} <span className="font-normal normal-case text-slate-400">{fmt(date)}</span>
              </h2>
              <div className="mt-3 space-y-3">
                {list.length === 0 && <p className="text-sm text-slate-400">Aucune séance</p>}
                {list.map((s) => {
                  const a = s.absence;
                  return (
                    <article key={s.sessionId} className={`rounded-xl border-l-4 p-3 text-sm ${a ? "border-red-500 bg-red-50" : "border-blue-500 bg-blue-50/60"}`}>
                      <p className={`font-semibold ${a ? "text-red-800 line-through decoration-red-300" : "text-slate-900"}`}>
                        {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}
                      </p>
                      <p className={`mt-1 font-medium ${a ? "text-red-900" : "text-slate-800"}`}>{s.subjectName}</p>
                      <p className="text-xs text-slate-500">{s.subjectCode} · Salle {s.room ?? "-"}</p>
                      {showClass && <p className="text-xs text-slate-600">{s.className}</p>}
                      {showProfessor && <p className="text-xs text-slate-600">{s.professorName}</p>}
                      {a && (
                        <p className="mt-2 text-xs font-semibold text-red-700">
                          Enseignant absent{a.reason ? ` · ${a.reason}` : ""}
                        </p>
                      )}

                      {mode !== "student" && openId !== s.sessionId && (
                        <div className="mt-2 flex flex-wrap gap-2">
                          {!a && (mode === "admin" || s.canDeclare) && (
                            <button onClick={() => { setOpenId(s.sessionId); setReason(""); }} className={smallBtn}>
                              {mode === "admin" ? "Marquer absent" : "Je serai absent(e)"}
                            </button>
                          )}
                          {a && (mode === "admin" || a.kind === "PROFESSOR") && (
                            <button onClick={() => withdraw(s)} disabled={busy} className={smallBtn}>
                              {mode === "admin" ? "Annuler l'absence" : "Annuler mon absence"}
                            </button>
                          )}
                          {mode === "professor" && !a && !s.canDeclare && s.date >= today && (
                            <span className="text-[11px] text-slate-400">Moins de 10 h avant la séance : contactez l&apos;administration.</span>
                          )}
                          {mode === "professor" && a?.kind === "ADMIN" && (
                            <span className="text-[11px] text-red-700">Signalée par l&apos;administration</span>
                          )}
                        </div>
                      )}
                      {openId === s.sessionId && (
                        <div className="mt-2 space-y-2">
                          <input value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)}
                            placeholder="Motif (facultatif)" className={`${inputCls} mt-0`} />
                          <div className="flex gap-2">
                            <button onClick={() => declare(s)} disabled={busy} className={primaryBtn}>Confirmer l&apos;absence</button>
                            <button onClick={() => setOpenId(null)} className={smallBtn}>Annuler</button>
                          </div>
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}