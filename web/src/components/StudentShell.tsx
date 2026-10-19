"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/student", label: "Tableau de bord", exact: true },
  { href: "/student/subjects", label: "Mes matières" },
  { href: "/student/courses", label: "Mes cours" },
  { href: "/student/timetable", label: "Emploi du temps" },
  { href: "/student/attendance", label: "Absences" },
  { href: "/student/grades", label: "Notes" },
  { href: "/student/assistant", label: "Assistant EduNex AI" },
  { href: "/student/notifications", label: "Notifications" },
  { href: "/student/profile", label: "Mon profil" },
];

export function StudentShell({ children }: { children: ReactNode }) {
  return (
    <RoleShell
      allowedRole="ROLE_STUDENT"
      badge="Espace étudiant"
      badgeClass="bg-sky-100 text-sky-700"
      nav={nav}
    >
      {children}
    </RoleShell>
  );
}