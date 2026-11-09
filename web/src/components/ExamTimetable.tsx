"use client";

import { useMemo, useState } from "react";
import { fullDate, hour } from "@/lib/exams";
import { smallBtn } from "./ui";

export type PlanItem = { key: string; date: string; start: string; end: string; title: string; detail: string; room: string | null; cancelled?: boolean; tag?: string };

const DAYS = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"];
const pad = (n: number) => String(n).padStart(2, "0");
const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const mondayOf = (d: Date) => addDays(new Date(d.getFullYear(), d.getMonth(), d.getDate(), 12), -((d.getDay() + 6) % 7));

function Card({ it }: { it: PlanItem }) {
  return (
    <article className={`rounded-xl border-l-4 p-3 text-sm ${it.cancelled ? "border-red-500 bg-red-50" : "border-violet-500 bg-violet-50/60"}`}>
      <p className={`font-semibold ${it.cancelled ? "text-red-800 line-through" : "text-slate-900"}`}>{hour(it.start)} - {hour(it.end)}</p>
      <p className="mt-1 font-medium text-slate-800">{it.title}</p>
      <p className="text-xs text-slate-500">{it.detail}{it.room ? ` · Salle ${it.room}` : ""}</p>
      {it.tag && <p className="mt-1 text-[11px] font-semibold text-violet-700">{it.tag}</p>}
      {it.cancelled && <p className="mt-1 text-xs font-semibold text-red-700">Examen annulé</p>}
    </article>
  );
}

/** Exams as a weekly timetable or as a list. */
export function ExamTimetable({ items, empty }: { items: PlanItem[]; empty: string }) {
  const [view, setView] = useState<"week" | "list">("week");
  const [monday, setMonday] = useState(() => mondayOf(new Date()));
  const [past, setPast] = useState(false);
  const today = iso(new Date());

  const byDate = useMemo(() => {
    const map = new Map<string, PlanItem[]>();
    items.forEach((i) => map.set(i.date, [...(map.get(i.date) ?? []), i]));
    map.forEach((v) => v.sort((a, b) => a.start.localeCompare(b.start)));
    return map;
  }, [items]);

  const days = Array.from({ length: 7 }, (_, i) => addDays(monday, i));
  const listDates = Array.from(byDate.keys()).filter((d) => past || d >= today).sort();
  const label = `${days[0].toLocaleDateString(undefined, { day: "2-digit", month: "short" })} - ${days[6].toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" })}`;

  return (
    <div>
      <div className="mt-6 flex flex-wrap items-center gap-3">
        <div className="inline-flex overflow-hidden rounded-xl border border-slate-200">
          {(["week", "list"] as const).map((v) => (
            <button key={v} onClick={() => setView(v)} className={`px-4 py-2 text-sm font-medium ${view === v ? "bg-blue-600 text-white" : "bg-white text-slate-700 hover:bg-slate-50"}`}>{v === "week" ? "Semaine" : "Liste"}</button>
          ))}
        </div>
        {view === "week" ? (
          <>
            <button onClick={() => setMonday(addDays(monday, -7))} className={smallBtn} aria-label="Précédent">←</button>
            <button onClick={() => setMonday(mondayOf(new Date()))} className={smallBtn}>Aujourd&apos;hui</button>
            <button onClick={() => setMonday(addDays(monday, 7))} className={smallBtn} aria-label="Suivant">→</button>
            <span className="text-sm font-semibold text-slate-700">{label}</span>
          </>
        ) : (
          <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={past} onChange={(e) => setPast(e.target.checked)} className="h-4 w-4 accent-blue-600" /> Afficher les examens passés</label>
        )}
      </div>

      {view === "week" && (
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4 2xl:grid-cols-7">
          {days.map((d, i) => {
            const list = byDate.get(iso(d)) ?? [];
            return (
              <section key={iso(d)} className={`rounded-2xl border bg-white p-4 shadow-sm ${iso(d) === today ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200"}`}>
                <h2 className="text-sm font-bold uppercase tracking-wide text-slate-500">{DAYS[i]} <span className="font-normal normal-case text-slate-400">{d.toLocaleDateString(undefined, { day: "2-digit", month: "short" })}</span></h2>
                <div className="mt-3 space-y-3">
                  {list.length === 0 && <p className="text-sm text-slate-400">Aucun examen</p>}
                  {list.map((it) => <Card key={it.key} it={it} />)}
                </div>
              </section>
            );
          })}
        </div>
      )}

      {view === "list" && (
        <div className="mt-4 space-y-5">
          {listDates.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">{empty}</p>}
          {listDates.map((d) => (
            <section key={d}>
              <h2 className="text-sm font-bold capitalize text-slate-600">{fullDate(d)}</h2>
              <div className="mt-2 grid gap-3 md:grid-cols-2 xl:grid-cols-3">{(byDate.get(d) ?? []).map((it) => <Card key={it.key} it={it} />)}</div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}