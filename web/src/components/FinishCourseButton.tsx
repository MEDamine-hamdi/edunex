"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

type Info = { completed: boolean; lastScore: number | null; lastTotal: number | null };

/** "Cours terminé": leads to the quiz page of the course. */
export function FinishCourseButton({ courseId }: { courseId: number }) {
  const [info, setInfo] = useState<Info | null>(null);

  useEffect(() => {
    apiFetch<Info>(`/api/student/courses/${courseId}/quiz-info`).then(setInfo).catch(() => {});
  }, [courseId]);

  return (
    <div className="mt-3 flex flex-wrap items-center gap-3">
      <Link href={`/student/courses/${courseId}/quiz`} className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700">
        {info?.completed ? "Refaire un quiz" : "Cours terminé : passer le quiz"}
      </Link>
      {info?.completed && (
        <span className="text-xs font-semibold text-emerald-700">
          Cours terminé{info.lastScore != null ? ` · dernier quiz ${info.lastScore}/${info.lastTotal}` : ""}
        </span>
      )}
    </div>
  );
}