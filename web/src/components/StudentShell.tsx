"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/student", label: "Tableau de bord", exact: true },
  { href: "/student/profile", label: "Mon profil" },
  { href: "#subjects", label: "Mes matières", soon: true },
  { href: "#courses", label: "Mes cours", soon: true },
  { href: "#timetable", label: "Emploi du temps", soon: true },
  { href: "#attendance", label: "Absences", soon: true },
  { href: "#grades", label: "Notes", soon: true },
  { href: "#ai", label: "Assistant EduNex AI", soon: true },
  { href: "#notifications", label: "Notifications", soon: true },
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