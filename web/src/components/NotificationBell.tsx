"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

export function NotificationBell({ href }: { href: string }) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let active = true;
    const load = () =>
      apiFetch<{ count: number }>("/api/notifications/unread-count")
        .then((r) => {
          if (active) setCount(r.count);
        })
        .catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    window.addEventListener("edunex:notifications", load);
    return () => {
      active = false;
      clearInterval(timer);
      window.removeEventListener("edunex:notifications", load);
    };
  }, []);

  return (
    <Link
      href={href}
      aria-label="Notifications"
      className="relative rounded-xl border border-slate-200 p-2.5 text-slate-600 hover:bg-slate-50"
    >
      <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M6 8a6 6 0 1 1 12 0c0 7 3 8 3 8H3s3-1 3-8" />
        <path d="M10.3 20a2 2 0 0 0 3.4 0" />
      </svg>
      {count > 0 && (
        <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold text-white">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}