"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { homeFor } from "@/lib/roles";

export default function DashboardRedirect() {
  const router = useRouter();

  useEffect(() => {
    apiFetch<{ role: string | null }>("/api/users/me")
      .then((me) => router.replace(homeFor(me.role)))
      .catch(() => {});
  }, [router]);

  return <p className="p-10 text-slate-500">Chargement...</p>;
}