"use client";

import type { Session } from "@/lib/types";
import { smallBtn } from "./ui";

export const DAYS: { key: string; label: string }[] = [
  { key: "MONDAY", label: "Lundi" },
  { key: "TUESDAY", label: "Mardi" },
  { key: "WEDNESDAY", label: "Mercredi" },
  { key: "THURSDAY", label: "Jeudi" },
  { key: "FRIDAY", label: "Vendredi" },
  { key: "SATURDAY", label: "Samedi" },
];

const time = (t: string) => t.slice(0, 5);

type Props = {
  sessions: Session[];
  showClass?: boolean;
  showProfessor?: boolean;
  onEdit?: (s: Session) => void;
  onDelete?: (s: Session) => void;
};

export function WeekGrid({ sessions, showClass, showProfessor, onEdit, onDelete }: Props) {
  const days = [...DAYS];
  if (sessions.some((s) => s.dayOfWeek === "SUNDAY")) days.push({ key: "SUNDAY", label: "Dimanche" });

  return (
    <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
      {days.map((d) => {
        const list = sessions
          .filter((s) => s.dayOfWeek === d.key)
          .sort((a, b) => a.startTime.localeCompare(b.startTime));
        return (
          <section key={d.key} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-sm font-bold uppercase tracking-wide text-slate-500">{d.label}</h2>
            <div className="mt-3 space-y-3">
              {list.length === 0 && <p className="text-sm text-slate-400">Aucune séance</p>}
              {list.map((s) => (
                <article key={s.id} className="rounded-xl border-l-4 border-blue-500 bg-blue-50/60 p-3 text-sm">
                  <p className="font-semibold text-slate-900">
                    {time(s.startTime)} - {time(s.endTime)}
                  </p>
                  <p className="mt-1 font-medium text-slate-800">{s.subjectName}</p>
                  <p className="text-xs text-slate-500">{s.subjectCode}</p>
                  <p className="mt-1 text-xs text-slate-600">Salle : {s.room ?? "-"}</p>
                  {showClass && <p className="text-xs text-slate-600">Classe : {s.className}</p>}
                  {showProfessor && <p className="text-xs text-slate-600">{s.professorName}</p>}
                  {(onEdit || onDelete) && (
                    <div className="mt-2 flex gap-2">
                      {onEdit && <button onClick={() => onEdit(s)} className={smallBtn}>Modifier</button>}
                      {onDelete && (
                        <button onClick={() => onDelete(s)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                      )}
                    </div>
                  )}
                </article>
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}