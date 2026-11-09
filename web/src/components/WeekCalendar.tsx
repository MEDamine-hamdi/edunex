"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, inputCls, primaryBtn, smallBtn } from "./ui";

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

type View = "day" | "week" | "month";

const DAY_NAMES = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"];
const pad = (n: number) => String(n).padStart(2, "0");
const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const noon = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate(), 12);
const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const mondayOf = (d: Date) => addDays(noon(d), -((d.getDay() + 6) % 7));

function rangeFor(view: View, anchor: Date): { from: Date; to: Date } {
  if (view === "day") return { from: anchor, to: anchor };
  if (view === "week") {
    const m = mondayOf(anchor);
    return { from: m, to: addDays(m, 6) };
  }
  const start = mondayOf(new Date(anchor.getFullYear(), anchor.getMonth(), 1, 12));
  return { from: start, to: addDays(start, 41) };
}

function explain(e: unknown): string {
  if (!(e instanceof ApiError)) return "Une erreur est survenue.";
  if (e.message.includes("at least")) return "Trop tard : une absence doit être déclarée au moins 10 heures avant la séance.";
  if (e.message.includes("already marked")) return "Cette séance est déjà marquée absente.";
  if (e.message.includes("Too late")) return "Trop tard pour retirer cette absence : contactez l'administration.";
  return e.message;
}

export function WeekCalendar({ endpoint, mode, query = "", showClass, showProfessor }: Props) {
  const [view, setView] = useState<View>("week");
  const [anchor, setAnchor] = useState(() => noon(new Date()));
  const [sessions, setSessions] = useState<WeekSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);

  const { from, to } = useMemo(() => rangeFor(view, anchor), [view, anchor]);
  const base = endpoint.replace(/\/week$/, "/range");

  const load = useCallback(async () => {
    try {
      setSessions(await apiFetch<WeekSession[]>(`${base}?from=${iso(from)}&to=${iso(to)}${query}`));
      setError(null);
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) setError(explain(e));
    } finally {
      setLoading(false);
    }
  }, [base, from, to, query]);

  useEffect(() => {
    load();
  }, [load]);

  const byDate = useMemo(() => {
    const map = new Map<string, WeekSession[]>();
    sessions.forEach((s) => map.set(s.date, [...(map.get(s.date) ?? []), s]));
    return map;
  }, [sessions]);

  const absenceBase = mode === "admin" ? "/api/admin/absences" : "/api/professor/absences";

  async function declare(s: WeekSession) {
    setBusy(true);
    setError(null);
    try {
      await apiFetch(absenceBase, { method: "POST", body: JSON.stringify({ sessionId: s.sessionId, date: s.date, reason: reason.trim() || null }) });
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
      await apiFetch<void>(`${absenceBase}/${s.absence.id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  function shift(n: number) {
    setAnchor((a) => (view === "month" ? new Date(a.getFullYear(), a.getMonth() + n, 1, 12) : addDays(a, n * (view === "week" ? 7 : 1))));
  }

  const fmt = (d: Date, o: Intl.DateTimeFormatOptions) => d.toLocaleDateString(undefined, o);
  const label =
    view === "day"
      ? fmt(anchor, { weekday: "long", day: "2-digit", month: "long", year: "numeric" })
      : view === "week"
        ? `${fmt(from, { day: "2-digit", month: "short" })} - ${fmt(to, { day: "2-digit", month: "short", year: "numeric" })}`
        : fmt(anchor, { month: "long", year: "numeric" });

  const key = (s: WeekSession) => `${s.sessionId}-${s.date}`;

  function card(s: WeekSession) {
    const a = s.absence;
    const id = key(s);
    return (
      <article key={id} className={`rounded-xl border-l-4 p-3 text-sm ${a ? "border-red-500 bg-red-50" : "border-blue-500 bg-blue-50/60"}`}>
        <p className={`font-semibold ${a ? "text-red-800 line-through decoration-red-300" : "text-slate-900"}`}>
          {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}
        </p>
        <p className={`mt-1 font-medium ${a ? "text-red-900" : "text-slate-800"}`}>{s.subjectName}</p>
        <p className="text-xs text-slate-500">{s.subjectCode} · Salle {s.room ?? "-"}</p>
        {showClass && <p className="text-xs text-slate-600">{s.className}</p>}
        {showProfessor && <p className="text-xs text-slate-600">{s.professorName}</p>}
        {a && <p className="mt-2 text-xs font-semibold text-red-700">Enseignant absent{a.reason ? ` · ${a.reason}` : ""}</p>}

        {mode !== "student" && openId !== id && (
          <div className="mt-2 flex flex-wrap gap-2">
            {!a && (mode === "admin" || s.canDeclare) && (
              <button onClick={() => { setOpenId(id); setReason(""); }} className={smallBtn}>
                {mode === "admin" ? "Marquer absent" : "Je serai absent(e)"}
              </button>
            )}
            {a && (mode === "admin" || a.kind === "PROFESSOR") && (
              <button onClick={() => withdraw(s)} disabled={busy} className={smallBtn}>
                {mode === "admin" ? "Annuler l'absence" : "Annuler mon absence"}
              </button>
            )}
            {mode === "professor" && !a && !s.canDeclare && s.date >= iso(new Date()) && (
              <span className="text-[11px] text-slate-400">Moins de 10 h avant la séance : contactez l&apos;administration.</span>
            )}
            {mode === "professor" && a?.kind === "ADMIN" && <span className="text-[11px] text-red-700">Signalée par l&apos;administration</span>}
          </div>
        )}
        {openId === id && (
          <div className="mt-2 space-y-2">
            <input value={reason} maxLength={500} onChange={(e) => setReason(e.target.value)} placeholder="Motif (facultatif)" className={`${inputCls} mt-0`} />
            <div className="flex gap-2">
              <button onClick={() => declare(s)} disabled={busy} className={primaryBtn}>Confirmer l&apos;absence</button>
              <button onClick={() => setOpenId(null)} className={smallBtn}>Annuler</button>
            </div>
          </div>
        )}
      </article>
    );
  }

  const today = iso(new Date());
  const showSunday = view === "week" && sessions.some((s) => s.dayOfWeek === "SUNDAY");
  const weekDays = Array.from({ length: showSunday ? 7 : 6 }, (_, i) => addDays(from, i));
  const monthDays = Array.from({ length: 42 }, (_, i) => addDays(from, i));

  return (
    <div>
      <div className="mt-6 flex flex-wrap items-center gap-3">
        <div className="inline-flex overflow-hidden rounded-xl border border-slate-200">
          {(["day", "week", "month"] as View[]).map((v) => (
            <button
              key={v}
              onClick={() => setView(v)}
              className={`px-4 py-2 text-sm font-medium ${view === v ? "bg-blue-600 text-white" : "bg-white text-slate-700 hover:bg-slate-50"}`}
            >
              {v === "day" ? "Jour" : v === "week" ? "Semaine" : "Mois"}
            </button>
          ))}
        </div>
        <button onClick={() => shift(-1)} className={smallBtn} aria-label="Précédent">←</button>
        <button onClick={() => setAnchor(noon(new Date()))} className={smallBtn}>Aujourd&apos;hui</button>
        <button onClick={() => shift(1)} className={smallBtn} aria-label="Suivant">→</button>
        <span className="text-sm font-semibold capitalize text-slate-700">{label}</span>
        <span className="ms-auto flex items-center gap-2 text-xs text-slate-500">
          <span className="h-3 w-3 rounded bg-red-500" /> Enseignant absent
        </span>
      </div>
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}

      {view === "day" && (
        <div className="mt-4 space-y-3">
          {(byDate.get(iso(anchor)) ?? []).length === 0 && !loading && (
            <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune séance</p>
          )}
          {(byDate.get(iso(anchor)) ?? []).map(card)}
        </div>
      )}

      {view === "week" && (
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
          {weekDays.map((d) => {
            const list = byDate.get(iso(d)) ?? [];
            return (
              <section key={iso(d)} className={`rounded-2xl border bg-white p-4 shadow-sm ${iso(d) === today ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200"}`}>
                <h2 className="text-sm font-bold uppercase tracking-wide text-slate-500">
                  {DAY_NAMES[(d.getDay() + 6) % 7]}{" "}
                  <span className="font-normal normal-case text-slate-400">{fmt(d, { day: "2-digit", month: "short" })}</span>
                </h2>
                <div className="mt-3 space-y-3">
                  {list.length === 0 && <p className="text-sm text-slate-400">Aucune séance</p>}
                  {list.map(card)}
                </div>
              </section>
            );
          })}
        </div>
      )}

      {view === "month" && (
        <div className="mt-4 overflow-x-auto">
          <div className="grid min-w-[44rem] grid-cols-7 gap-px overflow-hidden rounded-2xl border border-slate-200 bg-slate-200">
            {DAY_NAMES.map((n) => (
              <div key={n} className="bg-slate-50 px-2 py-2 text-center text-xs font-bold uppercase tracking-wide text-slate-500">{n}</div>
            ))}
            {monthDays.map((d) => {
              const list = byDate.get(iso(d)) ?? [];
              const inMonth = d.getMonth() === anchor.getMonth();
              return (
                <button
                  key={iso(d)}
                  onClick={() => { setAnchor(noon(d)); setView("day"); }}
                  className={`flex min-h-24 flex-col items-stretch gap-1 bg-white p-1.5 text-start align-top hover:bg-slate-50 ${inMonth ? "" : "opacity-50"}`}
                >
                  <span className={`self-end rounded-full px-1.5 text-xs font-semibold ${iso(d) === today ? "bg-blue-600 text-white" : "text-slate-600"}`}>{d.getDate()}</span>
                  {list.slice(0, 3).map((s) => (
                    <span
                      key={key(s)}
                      className={`block truncate rounded px-1 text-[11px] ${s.absence ? "bg-red-100 text-red-700 line-through" : "bg-blue-100 text-blue-800"}`}
                      title={`${s.startTime.slice(0, 5)} ${s.subjectName}`}
                    >
                      {s.startTime.slice(0, 5)} {s.subjectCode}
                    </span>
                  ))}
                  {list.length > 3 && <span className="text-[11px] text-slate-500">+{list.length - 3}</span>}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}