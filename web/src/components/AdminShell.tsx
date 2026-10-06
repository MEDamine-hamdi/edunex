"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/admin/professors", label: "Professeurs" },
  { href: "/admin/students", label: "Étudiants" },
  { href: "#classes", label: "Classes", soon: true },
  { href: "#sections", label: "Sections", soon: true },
  { href: "#subjects", label: "Matières", soon: true },
  { href: "#enrollments", label: "Inscriptions", soon: true },
  { href: "#timetables", label: "Emplois du temps", soon: true },
  { href: "#grades", label: "Notes", soon: true },
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