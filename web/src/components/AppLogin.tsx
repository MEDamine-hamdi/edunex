"use client";

import Link from "next/link";
import { FormEvent, ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/lib/auth";
import { homeFor } from "@/lib/roles";
import { AppLogo } from "./AppLogo";
import { LanguageSwitcher } from "./LanguageSwitcher";

function Outlined({ id, label, icon, type, value, onChange, trailing, autoComplete }: {
  id: string; label: string; icon: ReactNode; type: string; value: string; onChange: (v: string) => void; trailing?: ReactNode; autoComplete: string;
}) {
  return (
    <div className="relative">
      <span className="pointer-events-none absolute start-3.5 top-1/2 -translate-y-1/2 text-slate-500">{icon}</span>
      <input
        id={id}
        type={type}
        value={value}
        placeholder=" "
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        className="peer h-14 w-full rounded-xl border border-slate-200 bg-white ps-11 pe-12 text-base text-slate-900 outline-none focus:border-2 focus:border-blue-600"
      />
      <label
        htmlFor={id}
        className="pointer-events-none absolute start-10 top-1/2 -translate-y-1/2 bg-white px-1 text-base text-slate-500 transition-all peer-focus:top-0 peer-focus:start-3 peer-focus:text-xs peer-focus:text-blue-600 peer-[:not(:placeholder-shown)]:top-0 peer-[:not(:placeholder-shown)]:start-3 peer-[:not(:placeholder-shown)]:text-xs"
      >
        {label}
      </label>
      {trailing && <span className="absolute end-1 top-1/2 -translate-y-1/2">{trailing}</span>}
    </div>
  );
}

const MailIcon = (
  <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
);
const LockIcon = (
  <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
);

/** The sign-in screen of the mobile app, for phones and tablets. */
export function AppLogin() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError("Saisissez votre e-mail et votre mot de passe.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const data = await login(email.trim(), password, remember);
      router.replace(data.mustChangePassword ? "/change-password" : homeFor(data.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Une erreur est survenue.");
      setBusy(false);
    }
  }

  return (
    <div className="min-h-dvh bg-[linear-gradient(135deg,#FEF3C7,#FFE4E6,#E0E7FF)] dark:bg-none dark:bg-black">
      <div className="mx-auto flex min-h-dvh max-w-[480px] flex-col justify-center px-5 py-6">
        <div className="flex justify-end">
          <LanguageSwitcher />
        </div>
        <div className="mt-2 flex flex-col items-center">
          <AppLogo size={52} />
          <p className="mt-2 font-semibold text-slate-500">La plateforme de votre établissement</p>
        </div>
        <form onSubmit={submit} noValidate className="mt-6 rounded-2xl border border-slate-200 bg-white p-[22px]">
          <h1 className="text-2xl font-black text-slate-900">Connexion</h1>
          <p className="mb-5 mt-1 text-slate-500">Accédez à votre espace EduNex</p>
          <div className="space-y-3">
            <Outlined id="app-email" label="Adresse e-mail" icon={MailIcon} type="email" value={email} onChange={setEmail} autoComplete="username" />
            <Outlined
              id="app-password"
              label="Mot de passe"
              icon={LockIcon}
              type={show ? "text" : "password"}
              value={password}
              onChange={setPassword}
              autoComplete="current-password"
              trailing={
                <button type="button" onClick={() => setShow((v) => !v)} aria-label="Afficher le mot de passe" className="flex h-12 w-12 items-center justify-center rounded-full text-slate-600">
                  <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                    {show ? <><path d="M9.9 4.24A9 9 0 0 1 12 4c7 0 10 8 10 8a13 13 0 0 1-1.7 2.7M6.6 6.6A13 13 0 0 0 2 12s3 8 10 8a9.7 9.7 0 0 0 5.4-1.6M2 2l20 20" /></> : <><path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8Z" /><circle cx="12" cy="12" r="3" /></>}
                  </svg>
                </button>
              }
            />
          </div>
          <div className="mt-2 flex items-center justify-between">
            <label className="flex items-center gap-2 py-2 text-slate-800">
              <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} className="h-5 w-5 accent-blue-600" />
              Rester connecté
            </label>
            <Link href="/forgot-password" className="py-2 text-sm font-semibold text-blue-600">Mot de passe oublié ?</Link>
          </div>
          {error && <p className="mb-2 font-semibold text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={busy}
            className="mt-2 flex h-12 w-full items-center justify-center rounded-xl bg-indigo-600 font-bold text-white disabled:opacity-60"
          >
            {busy ? <span className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-white/40 border-t-white" /> : "Se connecter"}
          </button>
        </form>
      </div>
    </div>
  );
}