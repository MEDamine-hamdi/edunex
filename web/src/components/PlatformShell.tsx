"use client";

import { ReactNode, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { clearSession } from "@/lib/auth";
import { Logo } from "./Logo";

type Me = { firstName: string; lastName: string; email: string; role: string | null };

export function PlatformShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    apiFetch<Me>("/api/users/me")
      .then((data) => {
        if (data.role !== "ROLE_PLATFORM_ADMIN") router.replace("/dashboard");
        else setMe(data);
      })
      .catch(() => {});
  }, [router]);

  function logout() {
    clearSession();
    router.replace("/login");
  }

  if (!me) return <p className="p-10 text-slate-500">Chargement...</p>;

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-4">
        <div className="flex items-center gap-4">
          <Logo />
          <span className="rounded-full bg-violet-100 px-3 py-1 text-xs font-semibold text-violet-700">
            Administration de la plateforme
          </span>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-slate-600">{me.firstName} {me.lastName}</span>
          <button
            onClick={logout}
            className="rounded-xl border border-slate-200 px-4 py-2 font-medium text-slate-700 hover:bg-slate-50"
          >
            Se déconnecter
          </button>
        </div>
      </header>
      <div className="mx-auto max-w-6xl p-6">{children}</div>
    </div>
  );
}