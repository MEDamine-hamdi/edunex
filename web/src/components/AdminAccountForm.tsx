"use client";

import { FormEvent, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, inputCls, primaryBtn } from "./ui";

const FIELDS: { key: string; label: string; type?: string; required?: boolean }[] = [
  { key: "firstName", label: "Prénom", required: true },
  { key: "lastName", label: "Nom", required: true },
  { key: "email", label: "Adresse e-mail", type: "email", required: true },
  { key: "cin", label: "CIN (8 chiffres)", required: true },
  { key: "phone", label: "Téléphone" },
  { key: "employeeNumber", label: "Numéro d'employé", required: true },
  { key: "service", label: "Service" },
];

/** Creates an administrator: a temporary password is generated and e-mailed. */
export function AdminAccountForm({ endpoint, onCreated }: { endpoint: string; onCreated: () => void }) {
  const [form, setForm] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const body = Object.fromEntries(FIELDS.map((f) => [f.key, (form[f.key] ?? "").trim() || null]));
      await apiFetch(endpoint, { method: "POST", body: JSON.stringify(body) });
      setInfo(`Compte créé : un e-mail avec l'adresse de connexion et un mot de passe provisoire a été envoyé à ${form.email}.`);
      setForm({});
      onCreated();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.status === 409 ? "Cette adresse e-mail (ou ce numéro) est déjà utilisée." : err.status === 503 ? "L'envoi d'e-mails n'est pas disponible : le compte n'a pas été créé." : err.status === 400 ? `Vérifiez le formulaire : ${err.message}` : err.message);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <p className="mb-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600">Aucun mot de passe à saisir : un mot de passe provisoire est généré et envoyé par e-mail. La personne devra le remplacer à sa première connexion.</p>
      <ErrorBox message={error} />
      {info && <p className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}
      <div className="grid gap-4 sm:grid-cols-2">
        {FIELDS.map((f) => (
          <label key={f.key} className="block text-sm font-medium text-slate-700">
            {f.label}
            <input type={f.type ?? "text"} required={f.required} value={form[f.key] ?? ""} onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} className={inputCls} />
          </label>
        ))}
      </div>
      <button type="submit" disabled={busy} className={`mt-5 ${primaryBtn}`}>{busy ? "Création..." : "Créer le compte"}</button>
    </form>
  );
}