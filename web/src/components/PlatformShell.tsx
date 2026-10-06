"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/platform/agents", label: "Agents d'administration" },
  { href: "#roles", label: "Rôles", soon: true },
  { href: "#permissions", label: "Permissions", soon: true },
  { href: "#settings", label: "Paramètres", soon: true },
  { href: "#stats", label: "Statistiques", soon: true },
  { href: "#logs", label: "Journal d'activité", soon: true },
];

export function PlatformShell({ children }: { children: ReactNode }) {
  return (
    <RoleShell
      allowedRole="ROLE_PLATFORM_ADMIN"
      badge="Administration de la plateforme"
      badgeClass="bg-violet-100 text-violet-700"
      nav={nav}
    >
      {children}
    </RoleShell>
  );
}