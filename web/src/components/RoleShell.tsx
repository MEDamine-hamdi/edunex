"use client";

import { ReactNode, createContext, useContext, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { clearSession } from "@/lib/auth";
import { homeFor, notificationsFor, profileFor } from "@/lib/roles";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { Logo } from "./Logo";
import { NotificationBell } from "./NotificationBell";
import { ThemeSwitcher } from "./ThemeSwitcher";

export type Me = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: string | null;
  status: string;
};

export type NavItem = { href: string; label: string; soon?: boolean; exact?: boolean };

const MeContext = createContext<Me | null>(null);

export function useMe(): Me {
  const me = useContext(MeContext);
  if (!me) throw new Error("useMe must be used inside RoleShell");
  return me;
}

// Kept between page navigations so the layout does not flash "Chargement..." each time
let cachedMe: Me | null = null;

type Props = {
  allowedRole: string;
  badge: string;
  badgeClass: string;
  nav: NavItem[];
  children: ReactNode;
};

export function RoleShell({ allowedRole, badge, badgeClass, nav, children }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const mainRef = useRef<HTMLElement>(null);
  const [me, setMe] = useState<Me | null>(cachedMe?.role === allowedRole ? cachedMe : null);

  useEffect(() => {
    apiFetch<Me>("/api/users/me")
      .then((data) => {
        if (data.role === allowedRole) {
          cachedMe = data;
          setMe(data);
        } else {
          router.replace(homeFor(data.role));
        }
      })
      .catch(() => {});
  }, [allowedRole, router]);

  // a new page starts at the top of the content area
  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0 });
  }, [pathname]);

  function logout() {
    cachedMe = null;
    clearSession();
    router.replace("/login");
  }

  if (!me) return <p className="p-10 text-slate-500">Chargement...</p>;

  const isActive = (item: NavItem) =>
    item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(item.href + "/");

  const soonTag = (
    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase text-slate-500">
      Bientôt
    </span>
  );

  return (
    <MeContext.Provider value={me}>
      {/* The window never scrolls: header and sidebar stay in place, only <main> scrolls */}
      <div className="flex h-dvh flex-col overflow-hidden bg-slate-50 dark:bg-black">
        <header className="relative z-30 flex shrink-0 flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-3">
          <div className="flex flex-wrap items-center gap-4">
            <Link href={homeFor(me.role)} aria-label="Tableau de bord">
              <Logo />
            </Link>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${badgeClass}`}>{badge}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <LanguageSwitcher />
            <ThemeSwitcher />
            <NotificationBell href={notificationsFor(me.role)} />
            <Link href={profileFor(me.role)} className="hidden text-slate-600 hover:text-blue-700 sm:inline">
              {me.firstName} {me.lastName}
            </Link>
            <button
              onClick={logout}
              className="rounded-xl border border-slate-200 px-4 py-2 font-medium text-slate-700 hover:bg-slate-50"
            >
              Se déconnecter
            </button>
          </div>
        </header>

        {/* Mobile menu */}
        <nav className="flex shrink-0 gap-2 overflow-x-auto border-b border-slate-200 bg-white px-4 py-2 md:hidden">
          {nav.filter((i) => !i.soon).map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium ${
                isActive(item) ? "bg-blue-50 text-blue-700" : "text-slate-600"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex min-h-0 flex-1">
          {/* Sidebar: fixed, with its own scroll when it is too long */}
          <nav className="hidden w-64 shrink-0 overflow-y-auto overscroll-contain border-r border-slate-200 bg-white p-4 md:block">
            <ul className="space-y-1">
              {nav.map((item) => (
                <li key={item.label}>
                  {item.soon ? (
                    <span className="flex cursor-not-allowed items-center justify-between rounded-xl px-4 py-3 text-sm text-slate-400">
                      {item.label}
                      {soonTag}
                    </span>
                  ) : (
                    <Link
                      href={item.href}
                      className={`block rounded-xl px-4 py-3 text-sm font-medium ${
                        isActive(item) ? "bg-blue-50 text-blue-700" : "text-slate-600 hover:bg-slate-50"
                      }`}
                    >
                      {item.label}
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          </nav>

          <main ref={mainRef} className="min-w-0 flex-1 overflow-y-auto overscroll-contain p-6">
            {children}
          </main>
        </div>
      </div>
    </MeContext.Provider>
  );
}