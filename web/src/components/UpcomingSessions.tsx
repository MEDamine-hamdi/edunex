import { DAYS } from "./WeekGrid";
import type { Session } from "@/lib/types";

export function UpcomingSessions({ sessions, showProfessor }: { sessions: Session[]; showProfessor?: boolean }) {
  if (sessions.length === 0) return <p className="text-sm text-slate-500">Aucune séance planifiée.</p>;
  return (
    <ul className="space-y-3">
      {sessions.map((s) => {
        const day = DAYS.find((d) => d.key === s.dayOfWeek)?.label ?? s.dayOfWeek;
        return (
          <li key={s.id} className="rounded-xl border-l-4 border-blue-500 bg-blue-50/60 p-3 text-sm">
            <p className="font-semibold text-slate-900">
              {day} · {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}
            </p>
            <p className="text-slate-700">{s.subjectName} ({s.subjectCode})</p>
            <p className="text-xs text-slate-500">
              Salle {s.room ?? "-"} · {s.className}{showProfessor ? ` · ${s.professorName}` : ""}
            </p>
          </li>
        );
      })}
    </ul>
  );
}