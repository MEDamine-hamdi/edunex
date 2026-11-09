"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { useMe } from "./RoleShell";

const PATHS: Record<string, string> = {
  ROLE_STUDENT: "/student/messages",
  ROLE_PROFESSOR: "/professor/messages",
  ROLE_ADMIN_AGENT: "/admin/messages",
};

/** Envelope in the header with the number of unread messages. */
export function MessagesLink() {
  const me = useMe();
  const [unread, setUnread] = useState(0);
  const href = PATHS[me.role ?? ""];

  useEffect(() => {
    if (!href) return;
    let stop = false;
    const tick = () => {
      if (document.hidden) return;
      apiFetch<{ unread: number }>("/api/messaging/unread").then((r) => !stop && setUnread(r.unread)).catch(() => {});
    };
    tick();
    const t = setInterval(tick, 15000);
    return () => {
      stop = true;
      clearInterval(t);
    };
  }, [href]);

  if (!href) return null;
  return (
    <Link href={href} aria-label="Messagerie" title="Messagerie" className="relative rounded-xl border border-slate-200 bg-white p-2 text-slate-700 shadow-sm hover:bg-slate-50">
      <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      {unread > 0 && (
        <span className="absolute -end-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold text-white">{unread > 99 ? "99+" : unread}</span>
      )}
    </Link>
  );
}