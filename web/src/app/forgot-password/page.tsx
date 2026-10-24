"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { AuthLayout, authButton, authInput } from "@/components/AuthLayout";
import { publicPost } from "@/lib/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await publicPost("/api/auth/forgot-password", { email: email.trim() });
      setSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Une erreur est survenue.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout
      title="Mot de passe oublié"
      subtitle="Saisissez votre adresse e-mail : nous vous envoyons un lien pour choisir un nouveau mot de passe."
    >
      {sent ? (
        <div>
          <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            Si un compte correspond à cette adresse, un e-mail vient d&apos;être envoyé. Le lien est valable 30 minutes. Pensez à regarder dans vos courriers indésirables.
          </p>
          <Link href="/login" className="mt-6 inline-block text-sm font-semibold text-indigo-600 hover:underline">Retour à la connexion</Link>
        </div>
      ) : (
        <form onSubmit={submit}>
          {error && <p role="alert" className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
          <label htmlFor="email" className="block text-sm font-semibold text-stone-800">Adresse e-mail</label>
          <input id="email" type="email" required autoComplete="username" maxLength={254} value={email}
            onChange={(e) => setEmail(e.target.value)} placeholder="prenom.nom@universite.tn" className={authInput} />
          <button type="submit" disabled={busy} className={authButton}>{busy ? "Envoi..." : "Envoyer le lien"}</button>
          <Link href="/login" className="mt-5 inline-block text-sm font-semibold text-indigo-600 hover:underline">Retour à la connexion</Link>
        </form>
      )}
    </AuthLayout>
  );
}