"use client";

import { SectionLoader } from "@/components/Loading";
import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { BarList, Panel, StatCard } from "@/components/Stats";
import { ErrorBox, PageHeader } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type UniversityStats = {
  counts: {
    students: number;
    activeStudents: number;
    professors: number;
    classes: number;
    subjects: number;
    activeEnrollments: number;
    courses: number;
    documents: number;
    sessions: number;
  };
  enrollmentsByClass: { className: string; count: number }[];
  attendance: {
    present: number;
    absent: number;
    late: number;
    total: number;
    absenceRate: number;
    bySubject: { subjectName: string; absent: number; total: number; absenceRate: number }[];
  };
  grades: {
    count: number;
    average: number | null;
    distribution: { label: string; count: number }[];
    bySubject: { subjectName: string; average: number | null; count: number }[];
  };
};

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<UniversityStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<UniversityStats>("/api/admin/stats")
      .then(setStats)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401)
          setError(e.status === 403 ? "Vous n'avez pas la permission de consulter les statistiques." : e.message);
      });
  }, []);

  const c = stats?.counts;

  return (
    <AdminShell>
      <PageHeader title="Tableau de bord" subtitle="Vue d'ensemble de l'université." />
      <ErrorBox message={error} />
      {!stats && !error && <SectionLoader />}

      {stats && c && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Étudiants" value={c.students} hint={`${c.activeStudents} actifs`} tone="blue" />
            <StatCard label="Professeurs" value={c.professors} />
            <StatCard label="Classes" value={c.classes} />
            <StatCard label="Matières" value={c.subjects} />
            <StatCard label="Inscriptions actives" value={c.activeEnrollments} tone="green" />
            <StatCard label="Cours" value={c.courses} hint={`${c.documents} documents`} />
            <StatCard label="Séances planifiées" value={c.sessions} />
            <StatCard
              label="Moyenne générale"
              value={stats.grades.average == null ? "-" : `${stats.grades.average} / 20`}
              hint={`${stats.grades.count} notes`}
              tone={stats.grades.average != null && stats.grades.average < 10 ? "red" : "green"}
            />
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <Panel title="Inscriptions actives par classe">
              <BarList items={stats.enrollmentsByClass.map((x) => ({ label: x.className, value: x.count }))} />
            </Panel>
            <Panel title="Répartition des notes">
              <BarList items={stats.grades.distribution.map((x) => ({ label: `${x.label} / 20`, value: x.count }))} color="bg-violet-500" />
            </Panel>
            <Panel title="Moyenne par matière">
              <BarList
                items={stats.grades.bySubject.map((x) => ({ label: x.subjectName, value: x.average ?? 0 }))}
                max={20}
                suffix=" / 20"
                color="bg-emerald-500"
              />
            </Panel>
            <Panel title="Taux d'absence par matière">
              <p className="mb-4 text-sm text-slate-600">
                {stats.attendance.present} présences · {stats.attendance.absent} absences · {stats.attendance.late} retards
                {stats.attendance.total > 0 && ` · taux global ${stats.attendance.absenceRate} %`}
              </p>
              <BarList
                items={stats.attendance.bySubject.map((x) => ({ label: x.subjectName, value: x.absenceRate }))}
                max={100}
                suffix=" %"
                color="bg-red-500"
              />
            </Panel>
          </div>
        </>
      )}
    </AdminShell>
  );
}