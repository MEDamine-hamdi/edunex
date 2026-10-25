"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { AuthLayout, authButton, authInput } from "@/components/AuthLayout";
import { publicPost } from "@/lib/auth";

export default function ResetPasswordPage() {
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);

  // the secret link is read once, then removed from the address bar and the history
  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const t = new URLSearchParams(window.location.search).get("token");
    setToken(t);
    setReady(true);
    if (t) window.history.replaceState(null, "", window.location.pathname);
  }, []);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    if (password.length < 10 || password.length > 72 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError("Le mot de passe doit contenir 10 à 72 caractères, avec des lettres et des chiffres.");
      return;
    }
    if (password !== confirm) {
      setError("Les deux mots de passe ne sont pas identiques.");
      return;
    }
    setBusy(true);
    try {
      await publicPost("/api/auth/reset-password", { token, newPassword: password });
      setDone(true);
    } catch (err) {
      const status = (err as { status?: number }).status;
      setError(status === 400 ? "Ce lien n'est plus valable. Demandez un nouveau lien." : err instanceof Error ? err.message : "Une erreur est survenue.");
    } finally {
      setBusy(false);
    }
  }

  if (!ready) return null;

  return (
    <AuthLayout title="Nouveau mot de passe" subtitle={done || !token ? undefined : "Choisissez un mot de passe que vous n'utilisez nulle part ailleurs."}>
      {done ? (
        <div>
          <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            Votre mot de passe a été modifié. Vous pouvez vous connecter.
          </p>
          <Link href="/login" className={authButton}>Se connecter</Link>
        </div>
      ) : !token ? (
        <div>
          <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">Ce lien est incomplet ou n&apos;est plus valable.</p>
          <Link href="/forgot-password" className="mt-6 inline-block text-sm font-semibold text-indigo-600 hover:underline">Demander un nouveau lien</Link>
        </div>
      ) : (
        <form onSubmit={submit}>
          {error && <p role="alert" className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
          <label htmlFor="pw" className="block text-sm font-semibold text-stone-800">Nouveau mot de passe</label>
          <input id="pw" type="password" required autoComplete="new-password" minLength={10} maxLength={72} value={password}
            onChange={(e) => setPassword(e.target.value)} className={authInput} />
          <p className="mt-2 text-xs text-stone-500">10 à 72 caractères, avec des lettres et des chiffres.</p>
          <label htmlFor="pw2" className="mt-4 block text-sm font-semibold text-stone-800">Confirmer le mot de passe</label>
          <input id="pw2" type="password" required autoComplete="new-password" maxLength={72} value={confirm}
            onChange={(e) => setConfirm(e.target.value)} className={authInput} />
          <button type="submit" disabled={busy} className={authButton}>{busy ? "Enregistrement..." : "Réinitialiser le mot de passe"}</button>
        </form>
      )}
    </AuthLayout>
  );
}