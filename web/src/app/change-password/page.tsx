"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthLayout, authButton, authInput } from "@/components/AuthLayout";
import { LoadingScreen } from "@/components/Loading";
import { ApiError, apiFetch } from "@/lib/api";
import { logout } from "@/lib/auth";

export default function ChangePasswordPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [current, setCurrent] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<{ mustChangePassword: boolean }>("/api/users/me")
      .then((me) => {
        if (me.mustChangePassword) setReady(true);
        else router.replace("/dashboard");
      })
      .catch(() => {});
  }, [router]);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    if (password.length < 10 || password.length > 72 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError("Le nouveau mot de passe doit contenir 10 à 72 caractères, avec des lettres et des chiffres.");
      return;
    }
    if (password !== confirm) {
      setError("Les deux mots de passe ne sont pas identiques.");
      return;
    }
    setBusy(true);
    try {
      await apiFetch("/api/profile/first-password", {
        method: "PUT",
        body: JSON.stringify({ currentPassword: current, newPassword: password }),
      });
      router.replace("/dashboard");
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.message.includes("Incorrect password")) setError("Le mot de passe provisoire est incorrect.");
        else if (err.message.includes("must be different")) setError("Le nouveau mot de passe doit être différent du mot de passe provisoire.");
        else if (err.status === 429) setError("Trop de tentatives. Réessayez dans quelques minutes.");
        else setError(err.message);
      }
      setBusy(false);
    }
  }

  async function leave() {
    await logout();
    router.replace("/login");
  }

  if (!ready) return <LoadingScreen />;

  return (
    <AuthLayout
      title="Choisissez votre mot de passe"
      subtitle="Pour votre sécurité, remplacez le mot de passe provisoire reçu par e-mail par un mot de passe que vous êtes seul(e) à connaître."
    >
      <form onSubmit={submit}>
        {error && <p role="alert" className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
        <label htmlFor="current" className="block text-sm font-semibold text-stone-800">Mot de passe provisoire</label>
        <input id="current" type="password" required autoComplete="current-password" maxLength={128} value={current}
          onChange={(e) => setCurrent(e.target.value)} className={authInput} />
        <label htmlFor="pw" className="mt-4 block text-sm font-semibold text-stone-800">Nouveau mot de passe</label>
        <input id="pw" type="password" required autoComplete="new-password" minLength={10} maxLength={72} value={password}
          onChange={(e) => setPassword(e.target.value)} className={authInput} />
        <p className="mt-2 text-xs text-stone-500">10 à 72 caractères, avec des lettres et des chiffres.</p>
        <label htmlFor="pw2" className="mt-4 block text-sm font-semibold text-stone-800">Confirmer le mot de passe</label>
        <input id="pw2" type="password" required autoComplete="new-password" maxLength={72} value={confirm}
          onChange={(e) => setConfirm(e.target.value)} className={authInput} />
        <button type="submit" disabled={busy} className={authButton}>{busy ? "Enregistrement..." : "Enregistrer mon mot de passe"}</button>
        <button type="button" onClick={leave} className="mt-5 text-sm font-semibold text-indigo-600 hover:underline">Se déconnecter</button>
      </form>
    </AuthLayout>
  );
}