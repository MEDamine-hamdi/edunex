"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Setting = { id: number; key: string; value: string | null };

export default function SettingsPage() {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [newKey, setNewKey] = useState("");
  const [newValue, setNewValue] = useState("");

  const load = useCallback(async () => {
    try {
      const list = await apiFetch<Setting[]>("/api/platform/settings");
      setSettings(list);
      setDrafts(Object.fromEntries(list.map((s) => [s.key, s.value ?? ""])));
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

  async function save(key: string, value: string) {
    setError(null);
    setInfo(null);
    try {
      await apiFetch<Setting>(`/api/platform/settings/${encodeURIComponent(key)}`, {
        method: "PUT",
        body: JSON.stringify({ value }),
      });
      setInfo(`« ${key} » enregistré.`);
      await load();
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 400 ? "Clé invalide : lettres, chiffres, points, tirets ou underscores uniquement." : err.message);
    }
  }

  async function remove(key: string) {
    if (!window.confirm(`Supprimer le paramètre « ${key} » ?`)) return;
    setError(null);
    setInfo(null);
    try {
      await apiFetch<void>(`/api/platform/settings/${encodeURIComponent(key)}`, { method: "DELETE" });
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function add(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await save(newKey.trim(), newValue);
    setNewKey("");
    setNewValue("");
  }

  return (
    <PlatformShell>
      <PageHeader title="Paramètres" subtitle="Paramètres de configuration de la plateforme (clé / valeur)." />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <form onSubmit={add} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Ajouter ou modifier un paramètre</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-[1fr_2fr_auto] sm:items-end">
          <TextField label="Clé (ex. app.name)" maxLength={150} value={newKey} onChange={(e) => setNewKey(e.target.value)} required />
          <TextField label="Valeur" maxLength={2000} value={newValue} onChange={(e) => setNewValue(e.target.value)} />
          <button type="submit" className={`${primaryBtn} mb-0.5`}>Enregistrer</button>
        </div>
      </form>

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Clé</th>
              <th className="px-5 py-3">Valeur</th>
              <th className="px-5 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={3} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {!loading && settings.length === 0 && (
              <tr><td colSpan={3} className="px-5 py-8 text-center text-slate-500">Aucun paramètre pour le moment.</td></tr>
            )}
            {settings.map((s) => (
              <tr key={s.id}>
                <td className="px-5 py-3 font-mono text-xs text-slate-900">{s.key}</td>
                <td className="px-5 py-3">
                  <input
                    value={drafts[s.key] ?? ""}
                    onChange={(e) => setDrafts((d) => ({ ...d, [s.key]: e.target.value }))}
                    className={`${inputCls} mt-0 min-w-64`}
                  />
                </td>
                <td className="px-5 py-3 text-right">
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => save(s.key, drafts[s.key] ?? "")}
                      disabled={(drafts[s.key] ?? "") === (s.value ?? "")}
                      className={smallBtn}
                    >
                      Enregistrer
                    </button>
                    <button onClick={() => remove(s.key)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </PlatformShell>
  );
}