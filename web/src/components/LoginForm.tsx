"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { login, saveSession } from "@/lib/auth";
import { homeFor } from "@/lib/roles";
import { ArrowRightIcon, EyeIcon, EyeOffIcon, LockIcon, MailIcon } from "./icons";

export function LoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const data = await login(email.trim(), password);
      saveSession(data, remember);
      router.push(homeFor(data.role));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Une erreur est survenue. Réessayez.");
      setLoading(false);
    }
  }

  const field =
    "flex items-center gap-3 rounded-2xl border border-stone-200 bg-stone-50/80 px-4 py-4 text-stone-500 transition focus-within:border-indigo-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-indigo-100";
  const input = "w-full bg-transparent text-base text-stone-900 placeholder:text-stone-400 focus:outline-none";

  return (
    <form onSubmit={onSubmit} noValidate className="mt-8">
      {error && (
        <p role="alert" className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </p>
      )}

      <label htmlFor="email" className="mb-2 block text-sm font-semibold text-stone-800">Adresse e-mail</label>
      <div className={field}>
        <MailIcon className="h-5 w-5 shrink-0" />
        <input id="email" type="email" autoComplete="username" placeholder="prenom.nom@universite.tn"
          value={email} onChange={(e) => setEmail(e.target.value)} className={input} required />
      </div>

      <label htmlFor="password" className="mb-2 mt-5 block text-sm font-semibold text-stone-800">Mot de passe</label>
      <div className={field}>
        <LockIcon className="h-5 w-5 shrink-0" />
        <input id="password" type={showPassword ? "text" : "password"} autoComplete="current-password" placeholder="••••••••"
          value={password} onChange={(e) => setPassword(e.target.value)} className={input} required />
        <button type="button" onClick={() => setShowPassword((v) => !v)}
          aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
          className="text-stone-500 hover:text-stone-800">
          {showPassword ? <EyeOffIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
        </button>
      </div>

      <label className="mt-5 flex cursor-pointer items-center gap-3 text-sm text-stone-700">
        <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)}
          className="h-5 w-5 rounded border-stone-300 accent-indigo-600" />
        Se souvenir de moi
      </label>

      <button type="submit" disabled={loading}
        className="mt-7 flex w-full items-center justify-center gap-2 rounded-2xl bg-indigo-600 px-4 py-4 text-base font-semibold text-white shadow-lg shadow-indigo-600/25 transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-60">
        {loading ? "Connexion..." : "Se connecter"}
        {!loading && <ArrowRightIcon className="h-5 w-5" />}
      </button>

      <details className="mt-6 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-stone-700">
        <summary className="cursor-pointer font-semibold text-stone-800">Besoin d&apos;aide ?</summary>
        <p className="mt-2 leading-relaxed">
          Mot de passe oublié, premier accès ou compte bloqué ? Rapprochez-vous du secrétariat de votre établissement : l&apos;administration pourra vous aider.
        </p>
      </details>
    </form>
  );
}