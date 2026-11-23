import { AppLogo } from "@/components/AppLogo";
import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center bg-slate-50 px-6 text-center dark:bg-black">
      <AppLogo size={44} />
      <p className="mt-8 text-6xl font-black text-blue-600">404</p>
      <h1 className="mt-2 text-xl font-extrabold text-slate-900">Page introuvable</h1>
      <p className="mt-2 max-w-sm text-slate-500">Cette page n&apos;existe pas ou a été déplacée.</p>
      <Link href="/dashboard" className="mt-6 inline-flex h-12 items-center justify-center rounded-xl bg-blue-600 px-6 font-bold text-white">
        Retour à l&apos;accueil
      </Link>
    </main>
  );
}