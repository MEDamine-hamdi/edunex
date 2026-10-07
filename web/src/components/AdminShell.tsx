"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/admin/professors", label: "Professeurs" },
  { href: "/admin/students", label: "Étudiants" },
  { href: "/admin/classes", label: "Classes" },
  { href: "/admin/subjects", label: "Matières" },
  { href: "/admin/enrollments", label: "Inscriptions" },
  { href: "/admin/timetables", label: "Emplois du temps" },
  { href: "/admin/grades", label: "Notes" },
  { href: "#announcements", label: "Annonces", soon: true },
  { href: "#stats", label: "Statistiques", soon: true },
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