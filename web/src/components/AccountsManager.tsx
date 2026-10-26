"use client";

import { InlineLoader } from "@/components/Loading";
import { FormEvent, ReactNode, useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";

export type Status = "ACTIVE" | "INACTIVE" | "SUSPENDED";

const statusStyle: Record<Status, { label: string; cls: string }> = {
  ACTIVE: { label: "Actif", cls: "bg-emerald-100 text-emerald-700" },
  INACTIVE: { label: "Inactif", cls: "bg-slate-200 text-slate-600" },
  SUSPENDED: { label: "Suspendu", cls: "bg-orange-100 text-orange-700" },
};

export type FieldDef = {
  key: string;
  label: string;
  type?: "text" | "email" | "password" | "number";
  required?: boolean;
};

export type ColumnDef<T> = { label: string; render: (row: T) => ReactNode };

type Props<T> = {
  title: string;
  subtitle: string;
  endpoint: string;
  newLabel: string;
  fields: FieldDef[];
  columns: ColumnDef<T>[];
};

export function AccountsManager<T extends { id: number; status: Status }>({
  title,
  subtitle,
  endpoint,
  newLabel,
  fields,
  columns,
}: Props<T>) {
  const emptyForm = () => Object.fromEntries(fields.map((f) => [f.key, ""])) as Record<string, string>;

  const [rows, setRows] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<Record<string, string>>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setRows(await apiFetch<T[]>(endpoint));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [endpoint]);

  useEffect(() => {
    load();
  }, [load]);

  async function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setInfo(null);
    setSaving(true);
    const body: Record<string, unknown> = {};
    for (const f of fields.filter((x) => x.type !== "password")) {
      const raw = form[f.key] ?? "";
      if (f.type === "password") body[f.key] = raw;
      else {
        const v = raw.trim();
        body[f.key] = v === "" ? null : f.type === "number" ? Number(v) : v;
      }
    }
    try {
      const created = await apiFetch<T>(endpoint, { method: "POST", body: JSON.stringify(body) });
      setRows((prev) => [...prev, created]);
      setInfo("Compte créé : un e-mail contenant l'adresse de connexion et un mot de passe provisoire a été envoyé à " + String(body.email) + ".");
      setForm(emptyForm());
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409)
        setFormError("Cet email ou ce numéro est déjà utilisé.");
      else if (err instanceof ApiError) setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function toggle(row: T) {
    const next: Status = row.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    try {
      const updated = await apiFetch<T>(`${endpoint}/${row.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: next }),
      });
      setRows((prev) => prev.map((r) => (r.id === row.id ? updated : r)));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  const colCount = columns.length + 2;

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">{title}</h1>
          <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-medium text-white shadow-lg shadow-blue-600/25 hover:bg-blue-700"
        >
          {showForm ? "Fermer" : newLabel}
        </button>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {info && (
        <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>
      )}

      {showForm && (
        <form onSubmit={onCreate} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="mb-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600">
              Aucun mot de passe à saisir : un mot de passe provisoire est généré et envoyé par e-mail à la personne. Elle devra le remplacer dès sa première connexion.
            </p>
          {formError && (
            <p role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {formError}
            </p>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            {fields.filter((f) => f.type !== "password").map((f) => (
              <label key={f.key} className="block text-sm font-medium text-slate-700">
                {f.label}
                <input
                  type={f.type ?? "text"}
                  required={f.required}
                  minLength={f.type === "password" ? 8 : undefined}
                  autoComplete={f.type === "password" ? "new-password" : undefined}
                  value={form[f.key] ?? ""}
                  onChange={(e) => setForm((prev) => ({ ...prev, [f.key]: e.target.value }))}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[15px] text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100"
                />
              </label>
            ))}
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
              {columns.map((c) => (
                <th key={c.label} className="px-5 py-3">{c.label}</th>
              ))}
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr><td colSpan={colCount} className="px-5 py-8 text-center text-slate-500"><InlineLoader /></td></tr>
            )}
            {!loading && rows.length === 0 && (
              <tr><td colSpan={colCount} className="px-5 py-8 text-center text-slate-500">Aucun compte pour le moment.</td></tr>
            )}
            {rows.map((row) => {
              const s = statusStyle[row.status];
              return (
                <tr key={row.id}>
                  {columns.map((c) => (
                    <td key={c.label} className="px-5 py-4 text-slate-700">{c.render(row)}</td>
                  ))}
                  <td className="px-5 py-4">
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${s.cls}`}>{s.label}</span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button
                      onClick={() => toggle(row)}
                      className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
                    >
                      {row.status === "ACTIVE" ? "Désactiver" : "Activer"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
}