"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/professor", label: "Tableau de bord", exact: true },
  { href: "/professor/subjects", label: "Mes matières" },
  { href: "/professor/courses", label: "Mes cours" },
  { href: "/professor/classes", label: "Mes classes" },
  { href: "/professor/attendance", label: "Présences" },
  { href: "/professor/timetable", label: "Emploi du temps" },
  { href: "/professor/requests", label: "Demandes de documents" },
  { href: "/professor/notifications", label: "Notifications" },
  { href: "/professor/profile", label: "Mon profil" },
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