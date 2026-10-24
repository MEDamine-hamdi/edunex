"use client";

import { SectionLoader } from "@/components/Loading";
import Link from "next/link";
import { useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { useMe } from "@/components/RoleShell";
import { Panel, StatCard } from "@/components/Stats";
import { UpcomingSessions } from "@/components/UpcomingSessions";
import { ErrorBox, PageHeader, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Session } from "@/lib/types";

type Dashboard = {
  subjects: number;
  overallAverage: number | null;
  absences: number;
  lates: number;
  unreadNotifications: number;
  upcomingSessions: Session[];
  latestCourses: { id: number; title: string; subjectName: string; createdAt: string }[];
};

function Content() {
  const me = useMe();
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Dashboard>("/api/student/dashboard")
      .then(setData)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      });
  }, []);

  return (
    <>
      <PageHeader title={`Bonjour ${me.firstName}`} subtitle="Votre vie universitaire en un coup d'œil." />
      <ErrorBox message={error} />
      {!data && !error && <SectionLoader />}
      {data && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Matières" value={data.subjects} tone="blue" />
            <StatCard
              label="Moyenne générale"
              value={data.overallAverage == null ? "-" : `${data.overallAverage} / 20`}
              tone={data.overallAverage == null ? "slate" : data.overallAverage < 10 ? "red" : "green"}
            />
            <StatCard label="Absences" value={data.absences} tone={data.absences > 0 ? "red" : "slate"} />
            <StatCard label="Retards" value={data.lates} tone={data.lates > 0 ? "orange" : "slate"} />
            <Link href="/student/notifications" className="contents">
              <StatCard
                label="Notifications non lues"
                value={data.unreadNotifications}
                tone={data.unreadNotifications > 0 ? "orange" : "slate"}
              />
            </Link>
          </div>
          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <Panel title="Prochaines séances">
              <UpcomingSessions sessions={data.upcomingSessions} showProfessor />
              <Link href="/student/timetable" className={`mt-4 inline-block ${smallBtn}`}>Voir tout l&apos;emploi du temps</Link>
            </Panel>
            <Panel title="Derniers cours">
              {data.latestCourses.length === 0 ? (
                <p className="text-sm text-slate-500">Aucun cours disponible pour le moment.</p>
              ) : (
                <ul className="space-y-3">
                  {data.latestCourses.map((c) => (
                    <li key={c.id} className="rounded-xl bg-slate-50 px-4 py-3 text-sm">
                      <p className="font-medium text-slate-900">{c.title}</p>
                      <p className="text-xs text-slate-500">
                        {c.subjectName} · {new Date(c.createdAt).toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
              <Link href="/student/courses" className={`mt-4 inline-block ${smallBtn}`}>Tous mes cours</Link>
            </Panel>
          </div>
        </>
      )}
    </>
  );
}

export default function StudentHome() {
  return (
    <StudentShell>
      <Content />
    </StudentShell>
  );
}