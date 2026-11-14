"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { NavIcon, roleColor } from "@/lib/appNav";
import { homeFor } from "@/lib/roles";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { ThemeSwitcher } from "./ThemeSwitcher";
import { useMe, useShell } from "./RoleShell";

const card = "rounded-2xl border border-slate-200 bg-white";

/** The app's "Plus" tab: profile card, the other screens, language, theme and sign-out. */
export function MoreMenuPage() {
  const me = useMe();
  const { moreItems, logout } = useShell();
  const router = useRouter();
  const role = me.role ?? "";
  const color = roleColor(role);
  const initials = `${me.firstName?.[0] ?? ""}${me.lastName?.[0] ?? ""}`.toUpperCase();

  // on a computer the sidebar already shows everything
  useEffect(() => {
    if (window.matchMedia("(min-width: 1024px)").matches) router.replace(homeFor(me.role));
  }, [me.role, router]);

  return (
    <div className="space-y-3">
      <div className={`${card} flex items-center gap-3 p-4`}>
        <span className="flex h-12 w-12 items-center justify-center rounded-full text-[17px] font-extrabold" style={{ color, backgroundColor: color + "24" }}>
          {initials}
        </span>
        <div className="min-w-0">
          <p className="truncate text-base font-extrabold text-slate-900">{me.firstName} {me.lastName}</p>
          <p className="truncate text-[12.5px] text-slate-500">{me.email}</p>
        </div>
      </div>

      <div className="space-y-2">
        {moreItems.map((d) => (
          <Link key={d.href} href={d.href} className={`${card} flex items-center gap-3 p-3.5 active:bg-slate-50`}>
            <NavIcon name={d.key} role={role} className="h-6 w-6 shrink-0 text-blue-600" />
            <span className="min-w-0 flex-1 truncate text-[14.5px] font-bold text-slate-900">{d.label}</span>
            <NavIcon name="chevron" className="h-5 w-5 shrink-0 text-slate-400" />
          </Link>
        ))}
      </div>

      <div className={`${card} flex items-center gap-2.5 p-3`}>
        <NavIcon name="globe" className="h-6 w-6 text-slate-600" />
        <span className="flex-1 font-bold text-slate-900">Langue</span>
        <LanguageSwitcher />
      </div>
      <div className={`${card} flex items-center gap-2.5 p-3`}>
        <NavIcon name="sun" className="h-6 w-6 text-slate-600" />
        <span className="flex-1 font-bold text-slate-900">Thème</span>
        <ThemeSwitcher />
      </div>

      <button
        onClick={() => {
          if (window.confirm("Voulez-vous vous déconnecter ?")) logout();
        }}
        className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-slate-200 bg-transparent font-semibold text-red-600"
      >
        <NavIcon name="logout" className="h-5 w-5" />
        Se déconnecter
      </button>
    </div>
  );
}