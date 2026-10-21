"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/admin/dashboard", label: "Tableau de bord", exact: true },
  { href: "/admin/professors", label: "Professeurs" },
  { href: "/admin/students", label: "Étudiants" },
  { href: "/admin/classes", label: "Classes" },
  { href: "/admin/subjects", label: "Matières" },
  { href: "/admin/enrollments", label: "Inscriptions" },
  { href: "/admin/timetables", label: "Emplois du temps" },
  { href: "/admin/grades", label: "Notes" },
  { href: "/admin/announcements", label: "Annonces" },
  { href: "/admin/knowledge", label: "Base de connaissances" },
  { href: "/admin/requests", label: "Demandes de documents" },
  { href: "/admin/absences", label: "Absences des enseignants" },
  { href: "/admin/timetables/import", label: "Importer un emploi du temps (PDF)" },
  { href: "/admin/notifications", label: "Notifications" },
  { href: "/admin/profile", label: "Mon profil" },
];

export function AdminShell({ children }: { children: ReactNode }) {
  return (
    <RoleShell
      allowedRole="ROLE_ADMIN_AGENT"
      badge="Administration universitaire"
      badgeClass="bg-blue-100 text-blue-700"
      nav={nav}
    >
      {children}
    </RoleShell>
  );
}