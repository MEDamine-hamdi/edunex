"use client";

import { InlineLoader } from "@/components/Loading";
import { ChangeEvent, FormEvent, InputHTMLAttributes, useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ApiError, apiFetch } from "@/lib/api";

type Status = "ACTIVE" | "INACTIVE" | "SUSPENDED";

type Agent = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  employeeNumber: string;
  service: string | null;
  status: Status;
};

const emptyForm = {
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  phone: "",
  employeeNumber: "",
  service: "",
};

const statusStyle: Record<Status, { label: string; cls: string }> = {
  ACTIVE: { label: "Actif", cls: "bg-emerald-100 text-emerald-700" },
  INACTIVE: { label: "Inactif", cls: "bg-slate-200 text-slate-600" },
  SUSPENDED: { label: "Suspendu", cls: "bg-orange-100 text-orange-700" },
};

function Field({ label, ...props }: { label: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <input
        {...props}
        className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[15px] text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100"
      />
    </label>
  );
}

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setAgents(await apiFetch<Agent[]>("/api/platform/agents"));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const set = (key: keyof typeof emptyForm) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  async function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setSaving(true);
    try {
      const created = await apiFetch<Agent>("/api/platform/agents", {
        method: "POST",
        body: JSON.stringify({ ...form, phone: form.phone || null, service: form.service || null }),
      });
      setAgents((prev) => [...prev, created]);
      setForm(emptyForm);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409)
        setFormError("Cet email ou ce numéro d'employé est déjà utilisé.");
      else if (err instanceof ApiError) setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function toggle(agent: Agent) {
    const next: Status = agent.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      const updated = await apiFetch<Agent>(`/api/platform/agents/${agent.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: next }),
      });
      setAgents((prev) => prev.map((a) => (a.id === agent.id ? updated : a)));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <PlatformShell>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Agents d&apos;administration</h1>
          <p className="mt-1 text-sm text-slate-500">
            Créez et gérez les comptes des agents de l&apos;administration universitaire.
          </p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-medium text-white shadow-lg shadow-blue-600/25 hover:bg-blue-700"
        >
          {showForm ? "Fermer" : "+ Nouvel agent"}
        </button>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Nouvel agent</h2>
          {formError && (
            <p role="alert" className="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {formError}
            </p>
          )}
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Prénom" value={form.firstName} onChange={set("firstName")} required />
            <Field label="Nom" value={form.lastName} onChange={set("lastName")} required />
            <Field label="Email" type="email" value={form.email} onChange={set("email")} required />
            <Field
              label="Mot de passe (8 caractères min.)"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={form.password}
              onChange={set("password")}
              required
            />
            <Field label="Téléphone" value={form.phone} onChange={set("phone")} />
            <Field label="Numéro d'employé" value={form.employeeNumber} onChange={set("employeeNumber")} required />
            <Field label="Service" value={form.service} onChange={set("service")} />
          </div>
          <button
            type="submit"
            disabled={saving}
            className="mt-6 rounded-xl bg-blue-600 px-6 py-3 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {saving ? "Création..." : "Créer le compte"}
          </button>
        </form>
      )}

      <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-5 py-3">Nom</th>
              <th className="px-5 py-3">Email</th>
              <th className="px-5 py-3">N° employé</th>
              <th className="px-5 py-3">Service</th>
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500"><InlineLoader /></td></tr>
            )}
            {!loading && agents.length === 0 && (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Aucun agent pour le moment.</td></tr>
            )}
            {agents.map((a) => {
              const s = statusStyle[a.status];
              return (
                <tr key={a.id}>
                  <td className="px-5 py-4 font-medium">{a.firstName} {a.lastName}</td>
                  <td className="px-5 py-4 text-slate-600">{a.email}</td>
                  <td className="px-5 py-4 text-slate-600">{a.employeeNumber}</td>
                  <td className="px-5 py-4 text-slate-600">{a.service ?? "-"}</td>
                  <td className="px-5 py-4">
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${s.cls}`}>{s.label}</span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button
                      onClick={() => toggle(a)}
                      className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                    >
                      {a.status === "ACTIVE" ? "Désactiver" : "Activer"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </PlatformShell>
  );
}