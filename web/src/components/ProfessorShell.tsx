"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/professor", label: "Tableau de bord", exact: true },
  { href: "/professor/subjects", label: "Mes matières" },
  { href: "/professor/courses", label: "Mes cours" },
  { href: "/professor/profile", label: "Mon profil" },
  { href: "#classes", label: "Mes classes", soon: true },
  { href: "#attendance", label: "Présences", soon: true },
  { href: "/professor/timetable", label: "Emploi du temps" },
  { href: "#notifications", label: "Notifications", soon: true },
];

export function ProfessorShell({ children }: { children: ReactNode }) {
  return (
    <RoleShell
      allowedRole="ROLE_PROFESSOR"
      badge="Espace professeur"
      badgeClass="bg-emerald-100 text-emerald-700"
      nav={nav}
    >
      {children}
    </RoleShell>
  );
}