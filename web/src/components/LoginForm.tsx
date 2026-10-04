"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { login, saveSession } from "@/lib/auth";
import {
  ArrowRightIcon,
  EyeIcon,
  EyeOffIcon,
  GoogleIcon,
  LockIcon,
  MailIcon,
  MicrosoftIcon,
} from "./icons";

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
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Une erreur est survenue.");
      setLoading(false);
    }
  }

  const field =
    "flex items-center gap-3 rounded-xl border border-slate-200 bg-slate-50/70 px-4 py-3.5 text-slate-500 transition focus-within:border-blue-500 focus-within:bg-white focus-within:ring-4 focus-within:ring-blue-100";
  const input =
    "w-full bg-transparent text-[15px] text-slate-900 placeholder:text-slate-400 focus:outline-none";

  return (
    <form onSubmit={onSubmit} noValidate>
      <div className="mt-6 min-h-14">
        {error && (
          <p role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </p>
        )}
      </div>

      <label htmlFor="email" className="mb-2 block text-sm font-semibold text-slate-900">
        Email ou identifiant
      </label>
      <div className={field}>
        <MailIcon className="h-5 w-5 shrink-0" />
        <input
          id="email"
          type="email"
          autoComplete="username"
          placeholder="votre.email@universite.tn"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={input}
          required
        />
      </div>

      <label htmlFor="password" className="mb-2 mt-5 block text-sm font-semibold text-slate-900">
        Mot de passe
      </label>
      <div className={field}>
        <LockIcon className="h-5 w-5 shrink-0" />
        <input
          id="password"
          type={showPassword ? "text" : "password"}
          autoComplete="current-password"
          placeholder="••••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={input}
          required
        />
        <button
          type="button"
          onClick={() => setShowPassword((v) => !v)}
          aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
          className="text-slate-500 hover:text-slate-800"
        >
          {showPassword ? <EyeOffIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
        </button>
      </div>

      <div className="mt-4 flex items-center justify-between text-sm">
        <label className="flex cursor-pointer items-center gap-2.5 text-slate-700">
          <input
            type="checkbox"
            checked={remember}
            onChange={(e) => setRemember(e.target.checked)}
            className="h-5 w-5 rounded border-slate-300 accent-blue-600"
          />
          Se souvenir de moi
        </label>
        <a href="#" className="font-medium text-blue-600 underline underline-offset-2 hover:text-blue-700">
          Mot de passe oublié ?
        </a>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-4 text-base font-medium text-white shadow-lg shadow-blue-600/25 transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-200 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {loading ? "Connexion..." : "Se connecter"}
        {!loading && <ArrowRightIcon className="h-5 w-5" />}
      </button>

      <div className="my-6 flex items-center gap-4 text-sm text-slate-500">
        <span className="h-px flex-1 bg-slate-200" />
        ou continuer avec
        <span className="h-px flex-1 bg-slate-200" />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <button
          type="button"
          title="Bientôt disponible"
          className="flex items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white py-3.5 text-[15px] font-medium text-slate-800 transition hover:bg-slate-50"
        >
          <GoogleIcon className="h-5 w-5" /> Google
        </button>
        <button
          type="button"
          title="Bientôt disponible"
          className="flex items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white py-3.5 text-[15px] font-medium text-slate-800 transition hover:bg-slate-50"
        >
          <MicrosoftIcon className="h-5 w-5" /> Microsoft
        </button>
      </div>

      <p className="mt-6 text-center text-sm text-slate-600">
        Vous n&apos;avez pas de compte ?{" "}
        <a href="#" className="font-medium text-blue-600 underline underline-offset-2 hover:text-blue-700">
          Contacter l&apos;administration
        </a>
      </p>
    </form>
  );
}