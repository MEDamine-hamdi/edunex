"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { clearSession, getToken } from "@/lib/auth";

type Me = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string | null;
  status: string;
};

export default function DashboardPage() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      router.replace("/login");
      return;
    }
    fetch("/api/users/me", { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => {
        if (!res.ok) throw new Error("unauthorized");
        return res.json();
      })
      .then(setMe)
      .catch(() => {
        clearSession();
        router.replace("/login");
      });
  }, [router]);

  function logout() {
    clearSession();
    router.replace("/login");
  }

  if (!me) return <p className="p-10 text-slate-500">Chargement...</p>;

  return (
    <main className="mx-auto max-w-2xl p-10">
      <h1 className="text-3xl font-extrabold">Bonjour {me.firstName} {me.lastName}</h1>
      <p className="mt-2 text-slate-600">Connecté en tant que {me.email} ({me.role}).</p>
      <button
        onClick={logout}
        className="mt-6 rounded-xl bg-slate-900 px-5 py-3 text-white hover:bg-slate-700"
      >
        Se déconnecter
      </button>
    </main>
  );
}