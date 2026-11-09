"use client";

import { useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Hit = { id: number; name: string; email: string; role: string };
type Right = { code: string; description: string; fromRole: boolean; override: string | null; effective: boolean };
type View = { id: number; name: string; email: string; role: string; rights: Right[] };

const ROLE: Record<string, string> = { ROLE_PROFESSOR: "Professeur", ROLE_ADMIN_AGENT: "Agent" };

export default function UserRightsPage() {
  const [q, setQ] = useState("");
  const [hits, setHits] = useState<Hit[]>([]);
  const [view, setView] = useState<View | null>(null);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => apiFetch<Hit[]>(`/api/platform/user-rights/users?q=${encodeURIComponent(q)}`).then(setHits).catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.message)), 250);
    return () => clearTimeout(t);
  }, [q]);

  const open = useCallback(async (id: number) => {
    setInfo(null);
    setError(null);
    try {
      const v = await apiFetch<View>(`/api/platform/user-rights/users/${id}`);
      setView(v);
      setDraft(Object.fromEntries(v.rights.map((r) => [r.code, r.override ?? ""])));
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  }, []);

  async function save() {
    if (!view) return;
    setError(null);
    try {
      const v = await apiFetch<View>(`/api/platform/user-rights/users/${view.id}`, {
        method: "PUT",
        body: JSON.stringify({ overrides: Object.entries(draft).map(([code, effect]) => ({ code, effect })) }),
      });
      setView(v);
      setDraft(Object.fromEntries(v.rights.map((r) => [r.code, r.override ?? ""])));
      setInfo("Droits enregistrés. Ils s'appliquent dès la prochaine action de la personne.");
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  }

  const result = (r: Right) => (draft[r.code] === "GRANT" ? true : draft[r.code] === "DENY" ? false : r.fromRole);

  return (
    <PlatformShell>
      <PageHeader title="Droits par personne" subtitle="Donnez à une personne précise des droits en plus (ou en moins) de ceux de son rôle : par exemple, autoriser un professeur à saisir des notes ou à créer des emplois du temps." />
      <ErrorBox message={error} />
      <div className="mt-6 grid gap-6 lg:grid-cols-[320px_1fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher un professeur ou un agent" className={`${inputCls} mt-0`} />
          <ul className="mt-3 max-h-[60vh] space-y-1 overflow-y-auto">
            {hits.length === 0 && <li className="px-2 py-4 text-sm text-slate-500">Personne trouvée.</li>}
            {hits.map((h) => (
              <li key={h.id}>
                <button onClick={() => open(h.id)} className={`w-full rounded-xl px-3 py-2 text-start hover:bg-slate-50 ${view?.id === h.id ? "bg-blue-50" : ""}`}>
                  <span className="block text-sm font-medium text-slate-900">{h.name}</span>
                  <span className="text-xs text-slate-500">{ROLE[h.role] ?? h.role} · {h.email}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {!view ? (
            <p className="text-sm text-slate-500">Choisissez une personne dans la liste.</p>
          ) : (
            <>
              <h2 className="text-lg font-semibold">{view.name}</h2>
              <p className="text-sm text-slate-500">{ROLE[view.role] ?? view.role} · {view.email}</p>
              {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}
              <div className="mt-5 divide-y divide-slate-100">
                {view.rights.map((r) => (
                  <div key={r.code} className="flex flex-wrap items-center justify-between gap-3 py-3">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-slate-900">{r.description}</p>
                      <p className="text-xs text-slate-500">{r.code} · rôle : {r.fromRole ? "oui" : "non"}</p>
                    </div>
                    <select value={draft[r.code] ?? ""} onChange={(e) => setDraft({ ...draft, [r.code]: e.target.value })} className={`${inputCls} mt-0 w-44`}>
                      <option value="">Par défaut</option>
                      <option value="GRANT">Autoriser</option>
                      <option value="DENY">Refuser</option>
                    </select>
                    <span className={`w-20 rounded-full px-3 py-1 text-center text-xs font-semibold ${result(r) ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"}`}>{result(r) ? "Oui" : "Non"}</span>
                  </div>
                ))}
              </div>
              <button onClick={save} className={`mt-5 ${primaryBtn}`}>Enregistrer</button>
            </>
          )}
        </section>
      </div>
    </PlatformShell>
  );
}