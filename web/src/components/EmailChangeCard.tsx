"use client";

import { FormEvent, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { useMe } from "./RoleShell";
import { inputCls, primaryBtn } from "./ui";

export function EmailChangeCard() {
  const me = useMe();
  const [newEmail, setNewEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setInfo(null);
    setError(null);
    try {
      await apiFetch("/api/profile/email-change", { method: "POST", body: JSON.stringify({ newEmail: newEmail.trim(), password }) });
      setInfo("Si l'adresse est disponible, un e-mail de confirmation vient d'y être envoyé. Votre adresse actuelle ne change qu'après la confirmation, et vous devrez alors vous reconnecter.");
      setNewEmail("");
      setPassword("");
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 503) setError("L'envoi d'e-mails n'est pas disponible pour le moment.");
        else if (err.status === 429) setError("Trop de demandes. Réessayez plus tard.");
        else if (err.message.includes("Incorrect password")) setError("Mot de passe incorrect.");
        else if (err.message.includes("already your")) setError("C'est déjà votre adresse actuelle.");
        else if (err.status === 400) setError("Vérifiez l'adresse saisie et votre mot de passe.");
        else setError(err.message);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Changer mon adresse e-mail</h2>
      <p className="mt-1 text-sm text-slate-500">
        Adresse actuelle : <span className="font-medium text-slate-700">{me.email}</span>. C&apos;est aussi votre identifiant de connexion.
      </p>
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
        <label className="block text-sm font-medium text-slate-700">
          Nouvelle adresse e-mail
          <input type="email" required maxLength={254} value={newEmail} onChange={(e) => setNewEmail(e.target.value)} className={inputCls} />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Mot de passe actuel
          <input type="password" required autoComplete="current-password" maxLength={128} value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} />
        </label>
        <div className="sm:col-span-2">
          <button type="submit" disabled={busy} className={primaryBtn}>{busy ? "Envoi..." : "Envoyer la confirmation"}</button>
        </div>
      </form>
    </section>
  );
}