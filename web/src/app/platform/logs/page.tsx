"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Log = {
  id: number;
  action: string;
  entityType: string | null;
  timestamp: string;
  ipAddress: string | null;
  userEmail: string | null;
  userName: string | null;
};
type PageData = { items: Log[]; page: number; size: number; totalElements: number; totalPages: number };

const when = (iso: string) =>
  new Date(iso).toLocaleString("fr-FR", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" });

export default function LogsPage() {
  const [data, setData] = useState<PageData | null>(null);
  const [input, setInput] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const search = query ? `&q=${encodeURIComponent(query)}` : "";
      setData(await apiFetch<PageData>(`/api/platform/logs?page=${page}&size=25${search}`));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, [page, query]);

  useEffect(() => {
    load();
  }, [load]);

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPage(0);
    setQuery(input.trim());
  }

  return (
    <PlatformShell>
      <PageHeader title="Journal d'activité" subtitle="Connexions et modifications effectuées sur la plateforme." />
      <ErrorBox message={error} />

      <form onSubmit={submit} className="mt-6 flex max-w-xl gap-3">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Rechercher une action ou un email"
          className={`${inputCls} mt-0`}
        />
        <button type="submit" className={primaryBtn}>Rechercher</button>
        {query && (
          <button type="button" className={smallBtn} onClick={() => { setInput(""); setQuery(""); setPage(0); }}>
            Effacer
          </button>
        )}
      </form>

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Date</th>
              <th className="px-5 py-3">Utilisateur</th>
              <th className="px-5 py-3">Action</th>
              <th className="px-5 py-3">Entité</th>
              <th className="px-5 py-3">IP</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {!data && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {data && data.items.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucune entrée.</td></tr>
            )}
            {data?.items.map((l) => (
              <tr key={l.id}>
                <td className="whitespace-nowrap px-5 py-3 text-slate-600">{when(l.timestamp)}</td>
                <td className="px-5 py-3">
                  {l.userEmail ? (
                    <>
                      <span className="font-medium text-slate-900">{l.userName}</span>
                      <span className="block text-xs text-slate-500">{l.userEmail}</span>
                    </>
                  ) : (
                    <span className="text-slate-400">-</span>
                  )}
                </td>
                <td className={`px-5 py-3 font-mono text-xs ${l.action.startsWith("LOGIN_FAILED") ? "text-red-600" : "text-slate-700"}`}>
                  {l.action}
                </td>
                <td className="px-5 py-3 text-slate-600">{l.entityType ?? "-"}</td>
                <td className="px-5 py-3 text-slate-600">{l.ipAddress ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center gap-4 text-sm text-slate-600">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className={smallBtn}>Précédent</button>
          <span>Page {data.page + 1} / {data.totalPages} · {data.totalElements} entrées</span>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)} className={smallBtn}>Suivant</button>
        </div>
      )}
    </PlatformShell>
  );
}