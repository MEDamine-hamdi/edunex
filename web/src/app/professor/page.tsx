"use client";

import { SectionLoader } from "@/components/Loading";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { useMe } from "@/components/RoleShell";
import { Panel, StatCard } from "@/components/Stats";
import { UpcomingSessions } from "@/components/UpcomingSessions";
import { ErrorBox, PageHeader, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Session } from "@/lib/types";

type Dashboard = {
  subjects: number;
  courses: number;
  documents: number;
  classes: number;
  students: number;
  unreadNotifications: number;
  upcomingSessions: Session[];
};

function Content() {
  const me = useMe();
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Dashboard>("/api/professor/dashboard")
      .then(setData)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      });
  }, []);

  return (
    <>
      <PageHeader title={`Bonjour ${me.firstName}`} subtitle="Votre activité d'enseignement." />
      <ErrorBox message={error} />
      {!data && !error && <SectionLoader />}
      {data && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <StatCard label="Matières" value={data.subjects} tone="green" />
            <StatCard label="Cours publiés" value={data.courses} hint={`${data.documents} documents`} />
            <StatCard label="Classes" value={data.classes} />
            <StatCard label="Étudiants" value={data.students} />
            <Link href="/professor/notifications" className="contents">
              <StatCard
                label="Notifications non lues"
                value={data.unreadNotifications}
                tone={data.unreadNotifications > 0 ? "orange" : "slate"}
              />
            </Link>
          </div>
          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <Panel title="Prochaines séances">
              <UpcomingSessions sessions={data.upcomingSessions} />
              <Link href="/professor/timetable" className={`mt-4 inline-block ${smallBtn}`}>Voir tout l&apos;emploi du temps</Link>
            </Panel>
            <Panel title="Raccourcis">
              <div className="flex flex-wrap gap-3">
                <Link href="/professor/attendance" className={smallBtn}>Faire l&apos;appel</Link>
                <Link href="/professor/courses" className={smallBtn}>Mes cours</Link>
                <Link href="/professor/classes" className={smallBtn}>Mes classes</Link>
              </div>
            </Panel>
          </div>
        </>
      )}
    </>
  );
}

export default function ProfessorHome() {
  return (
    <ProfessorShell>
      <Content />
    </ProfessorShell>
  );
}