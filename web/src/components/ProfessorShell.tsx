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
  { href: "/professor/internships", label: "Stages et soutenances" },
  { href: "/professor/messages", label: "Messagerie" },
  { href: "/professor/delegated/grades", label: "Saisie des notes", requires: "GRADES_MANAGE" },
  { href: "/professor/delegated/timetable", label: "Gestion des emplois du temps", requires: "TIMETABLE_MANAGE" },
  { href: "/professor/exams", label: "Surveillance des examens" },
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