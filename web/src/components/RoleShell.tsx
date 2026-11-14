"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { createContext, ReactNode, useContext, useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import { logout as endSession } from "@/lib/auth";
import { NavIcon, MORE_ORDER, PRIMARY, appLabel, bottomLabel, keyOf, roleColor, roleLabel } from "@/lib/appNav";
import { homeFor, notificationsFor, profileFor } from "@/lib/roles";
import { AppLogo } from "./AppLogo";
import { LanguageSwitcher } from "./LanguageSwitcher";
import { Logo } from "./Logo";
import { NotificationBell } from "./NotificationBell";
import { ThemeSwitcher } from "./ThemeSwitcher";
import { MessagesLink } from "./MessagesLink";
import { EliminationBanner } from "./EliminationBanner";
import { LoadingScreen } from "./Loading";

export type Me = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: string | null;
  status: string;
  mustChangePassword?: boolean;
};

export type NavItem = { href: string; label: string; soon?: boolean; exact?: boolean; requires?: string };
export type MoreItem = { href: string; label: string; key: string };

const MeContext = createContext<Me | null>(null);
const ShellContext = createContext<{ moreItems: MoreItem[]; logout: () => void } | null>(null);

export function useMe(): Me {
  const me = useContext(MeContext);
  if (!me) throw new Error("useMe must be used inside RoleShell");
  return me;
}

export function useShell() {
  const s = useContext(ShellContext);
  if (!s) throw new Error("useShell must be used inside RoleShell");
  return s;
}

// Kept between page navigations so the layout does not flash a loading screen each time
let cachedMe: Me | null = null;
const HAS_MESSAGING = true;

type Props = {
  allowedRole: string;
  badge: string;
  badgeClass: string;
  nav: NavItem[];
  children: ReactNode;
};

function AppBarButton({ href, label, count, children }: { href: string; label: string; count: number; children: ReactNode }) {
  return (
    <Link href={href} aria-label={label} className="relative flex h-12 w-12 items-center justify-center rounded-full text-slate-700 active:bg-slate-100">
      {children}
      {count > 0 && (
        <span className="absolute end-1.5 top-2 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold leading-none text-white">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}

export function RoleShell({ allowedRole, badge, badgeClass, nav, children }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const mainRef = useRef<HTMLElement>(null);
  const [me, setMe] = useState<Me | null>(cachedMe?.role === allowedRole ? cachedMe : null);
  const [perms, setPerms] = useState<string[]>([]);
  const [unread, setUnread] = useState(0);
  const [msgUnread, setMsgUnread] = useState(0);

  useEffect(() => {
    apiFetch<Me>("/api/users/me")
      .then((data) => {
        if (data.role !== allowedRole) {
          router.replace(homeFor(data.role));
          return;
        }
        if (data.mustChangePassword) {
          router.replace("/change-password");
          return;
        }
        cachedMe = data;
        setMe(data);
      })
      .catch(() => {});
  }, [allowedRole, router]);

  // extra rights given to this person make some menu entries appear
  useEffect(() => {
    apiFetch<string[]>("/api/users/me/permissions").then(setPerms).catch(() => {});
  }, []);

  // a new page starts at the top of the content area
  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0 });
  }, [pathname]);

  // unread counters of the app bar (the app polls every 30 seconds)
  useEffect(() => {
    if (!me) return;
    let stop = false;
    const tick = () => {
      if (document.hidden) return;
      apiFetch<{ count: number }>("/api/notifications/unread-count").then((r) => !stop && setUnread(r.count ?? 0)).catch(() => {});
      if (HAS_MESSAGING && me.role !== "ROLE_PLATFORM_ADMIN") {
        apiFetch<{ unread: number }>("/api/messaging/unread").then((r) => !stop && setMsgUnread(r.unread ?? 0)).catch(() => {});
      }
    };
    tick();
    const t = setInterval(tick, 30000);
    return () => {
      stop = true;
      clearInterval(t);
    };
  }, [me, pathname]);

  async function logout() {
    cachedMe = null;
    await endSession();
    router.replace("/login");
  }

  if (!me) return <LoadingScreen />;

  const role = me.role ?? "";
  const home = homeFor(me.role);
  const prefix = home.split("/")[1] ?? "";
  const moreHref = `/${prefix}/more`;

  // ---- navigation model shared by the phone layout and the desktop layout
  const allowed = nav.filter((i) => !i.requires || perms.includes(i.requires));
  const visible = allowed.filter((i) => !i.soon);
  const keyFor = (i: NavItem) => (i.href === home ? "dashboard" : keyOf(i.href) || "dashboard");
  const keyed = visible.map((item) => ({ item, key: keyFor(item) }));

  const primary: { item: NavItem; key: string }[] = [];
  for (const k of PRIMARY[role] ?? []) {
    const found = keyed.find((x) => x.key === k);
    if (found) primary.push(found);
  }
  for (const x of keyed) {
    if (primary.length >= 4) break;
    if (!primary.includes(x) && x.key !== "messages") primary.push(x);
  }
  const rest = keyed.filter((x) => !primary.includes(x) && x.key !== "messages");
  const has = (key: string) => primary.some((x) => x.key === key) || rest.some((x) => x.key === key);
  if (!has("notifications")) rest.push({ item: { href: notificationsFor(me.role), label: "Notifications" }, key: "notifications" });
  if (!has("profile")) rest.push({ item: { href: profileFor(me.role), label: "Mon profil" }, key: "profile" });
  const order = MORE_ORDER[role] ?? [];
  const rank = (key: string) => (order.indexOf(key) < 0 ? 999 : order.indexOf(key));
  const moreItems: MoreItem[] = [...rest]
    .sort((a, b) => rank(a.key) - rank(b.key))
    .map((x) => ({ href: x.item.href, label: appLabel(role, x.key, x.item.label), key: x.key }));

  const onMore = pathname === moreHref;
  const primaryIdx = primary.findIndex((x) => x.item.href === pathname);
  const showBottomNav = onMore || primaryIdx >= 0;
  const isMessages = pathname === `/${prefix}/messages`;
  const exact = [...keyed, ...moreItems.map((m) => ({ item: { href: m.href, label: m.label } as NavItem, key: m.key }))].find((x) => x.item.href === pathname);
  const longest = [...keyed]
    .filter((x) => x.item.href !== home && pathname.startsWith(x.item.href + "/"))
    .sort((a, b) => b.item.href.length - a.item.href.length)[0];
  const isHome = primaryIdx >= 0 && primary[primaryIdx].key === "dashboard";
  const titleShown = onMore || isMessages || !!exact;
  const title = onMore ? "Plus" : isMessages ? "Messagerie" : exact ? appLabel(role, exact.key, exact.item.label) : longest ? appLabel(role, longest.key, longest.item.label) : "EduNex";
  const showBack = !showBottomNav;
  const goBack = () => {
    if (window.history.length > 1) router.back();
    else router.push(home);
  };
  const color = roleColor(role);

  const isActive = (item: NavItem) =>
    item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(item.href + "/");
  const soonTag = (
    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase text-slate-500">Bientôt</span>
  );

  return (
    <MeContext.Provider value={me}>
      <ShellContext.Provider value={{ moreItems, logout }}>
        {/* The window never scrolls: bars stay in place, only <main> scrolls */}
        <div className="flex h-dvh flex-col overflow-hidden bg-slate-50 dark:bg-black">
          {/* ---------- phone and tablet: the app bar ---------- */}
          <header
            className="flex h-14 shrink-0 items-center border-b border-slate-200 bg-white ps-2 pe-1 lg:hidden"
            style={{ paddingTop: "env(safe-area-inset-top)", height: "calc(3.5rem + env(safe-area-inset-top))" }}
          >
            {showBack && (
              <button onClick={goBack} aria-label="Retour" className="flex h-12 w-12 items-center justify-center rounded-full text-slate-800 active:bg-slate-100">
                <NavIcon name="back" />
              </button>
            )}
            <div className={`min-w-0 flex-1 ${showBack ? "" : "ps-2"}`}>
              {isHome ? <AppLogo size={30} /> : <span className="block truncate text-[18px] font-extrabold text-slate-900">{title}</span>}
            </div>
            {HAS_MESSAGING && role !== "ROLE_PLATFORM_ADMIN" && (
              <AppBarButton href={`/${prefix}/messages`} label="Messagerie" count={msgUnread}>
                <NavIcon name="messages" />
              </AppBarButton>
            )}
            <AppBarButton href={notificationsFor(me.role)} label="Notifications" count={unread}>
              <NavIcon name="notifications" />
            </AppBarButton>
            <span className="me-2 rounded-lg px-2 py-[3px] text-[11px] font-extrabold" style={{ color, backgroundColor: color + "21" }}>
              {roleLabel(role)}
            </span>
          </header>

          {/* ---------- computer: the header ---------- */}
          <header className="relative z-30 hidden shrink-0 flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-3 lg:flex">
            <div className="flex flex-wrap items-center gap-4">
              <Link href={home} aria-label="Tableau de bord">
                <Logo />
              </Link>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${badgeClass}`}>{badge}</span>
            </div>
            <div className="flex items-center gap-3 text-sm">
              <LanguageSwitcher />
              <ThemeSwitcher />
              <MessagesLink />
              <NotificationBell href={notificationsFor(me.role)} />
              <Link href={profileFor(me.role)} className="hidden text-slate-600 hover:text-blue-700 sm:inline">
                {me.firstName} {me.lastName}
              </Link>
              <button onClick={logout} className="rounded-xl border border-slate-200 px-4 py-2 font-medium text-slate-700 hover:bg-slate-50">
                Se déconnecter
              </button>
            </div>
          </header>

          <div className="flex min-h-0 flex-1">
            {/* Sidebar: computer only */}
            <nav className="hidden w-64 shrink-0 overflow-y-auto overscroll-contain border-r border-slate-200 bg-white p-4 lg:block">
              <ul className="space-y-1">
                {allowed.map((item) => (
                  <li key={item.label}>
                    {item.soon ? (
                      <span className="flex cursor-not-allowed items-center justify-between rounded-xl px-4 py-3 text-sm text-slate-400">
                        {item.label}
                        {soonTag}
                      </span>
                    ) : (
                      <Link
                        href={item.href}
                        className={`block rounded-xl px-4 py-3 text-sm font-medium ${isActive(item) ? "bg-blue-50 text-blue-700" : "text-slate-600 hover:bg-slate-50"}`}
                      >
                        {item.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </nav>

            <main
              ref={mainRef}
              data-title-shown={titleShown ? "true" : "false"}
              className={`min-w-0 flex-1 overflow-y-auto overscroll-contain p-4 lg:p-6 ${showBottomNav ? "pb-6" : "pb-10"} lg:pb-6`}
            >
              {me.role === "ROLE_STUDENT" && <EliminationBanner />}
              {children}
            </main>
          </div>

          {/* ---------- phone and tablet: the bottom navigation bar ---------- */}
          {showBottomNav && (
            <nav
              className="flex shrink-0 bg-white shadow-[0_-2px_8px_rgba(15,23,42,0.07)] lg:hidden"
              style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
              aria-label="Navigation"
            >
              {primary.map((x, idx) => {
                const active = idx === primaryIdx;
                return (
                  <Link key={x.item.href} href={x.item.href} className="flex h-20 min-w-0 flex-1 flex-col items-center justify-center gap-1">
                    <span className={`flex h-8 w-16 items-center justify-center rounded-full ${active ? "bg-blue-600/15 dark:bg-blue-600/30" : ""}`}>
                      <NavIcon name={x.key} role={role} className={`h-6 w-6 ${active ? "text-slate-900" : "text-slate-500"}`} />
                    </span>
                    <span className={`max-w-full truncate px-1 text-[11px] font-semibold ${active ? "text-slate-900" : "text-slate-500"}`}>
                      {bottomLabel(appLabel(role, x.key, x.item.label))}
                    </span>
                  </Link>
                );
              })}
              <Link href={moreHref} className="flex h-20 min-w-0 flex-1 flex-col items-center justify-center gap-1">
                <span className={`flex h-8 w-16 items-center justify-center rounded-full ${primaryIdx < 0 ? "bg-blue-600/15 dark:bg-blue-600/30" : ""}`}>
                  <NavIcon name="apps" className={`h-6 w-6 ${primaryIdx < 0 ? "text-slate-900" : "text-slate-500"}`} />
                </span>
                <span className={`text-[11px] font-semibold ${primaryIdx < 0 ? "text-slate-900" : "text-slate-500"}`}>Plus</span>
              </Link>
            </nav>
          )}
        </div>
      </ShellContext.Provider>
    </MeContext.Provider>
  );
}