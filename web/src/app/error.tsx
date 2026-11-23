"use client";

import { AppLogo } from "@/components/AppLogo";
import Link from "next/link";
import { useEffect } from "react";

export default function ErrorPage({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center bg-slate-50 px-6 text-center dark:bg-black">
      <AppLogo size={44} />
      <h1 className="mt-8 text-xl font-extrabold text-slate-900">Une erreur est survenue</h1>
      <p className="mt-2 max-w-sm text-slate-500">Quelque chose s&apos;est mal passé. Vous pouvez réessayer ou revenir à l&apos;accueil.</p>
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <button onClick={() => reset()} className="inline-flex h-12 items-center justify-center rounded-xl bg-blue-600 px-6 font-bold text-white">
          Réessayer
        </button>
        <Link href="/dashboard" className="inline-flex h-12 items-center justify-center rounded-xl border border-slate-200 px-6 font-semibold text-slate-700">
          Retour à l&apos;accueil
        </Link>
      </div>
    </main>
  );
}