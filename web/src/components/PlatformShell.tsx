"use client";

import { ReactNode } from "react";
import { NavItem, RoleShell } from "./RoleShell";

const nav: NavItem[] = [
  { href: "/platform/dashboard", label: "Tableau de bord", exact: true },
  { href: "/platform/agents", label: "Agents d'administration" },
  { href: "/platform/roles", label: "Rôles" },
  { href: "/platform/permissions", label: "Permissions" },
  { href: "/platform/settings", label: "Paramètres" },
  { href: "/platform/logs", label: "Journal d'activité" },
  { href: "/platform/notifications", label: "Notifications" },
  { href: "/platform/profile", label: "Mon profil" },
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