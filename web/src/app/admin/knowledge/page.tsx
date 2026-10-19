"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { downloadFromUrl } from "@/lib/download";
import { formatDate, formatSize } from "@/lib/format";

type Doc = {
  id: number;
  title: string;
  type: string | null;
  size: number | null;
  status: "PENDING" | "INDEXED" | "EMPTY" | "UNSUPPORTED" | "FAILED";
  chunkCount: number;
  uploadedAt: string;
  uploadedBy: string | null;
};

const STATUS: Record<Doc["status"], { label: string; cls: string; hint: string }> = {
  INDEXED: { label: "Prêt", cls: "bg-emerald-100 text-emerald-700", hint: "Utilisé par l'assistant" },
  PENDING: { label: "En attente", cls: "bg-slate-100 text-slate-600", hint: "Analyse en cours" },
  EMPTY: { label: "Sans texte", cls: "bg-orange-100 text-orange-700", hint: "Aucun texte lisible (PDF scanné ?)" },
  UNSUPPORTED: { label: "Non lisible", cls: "bg-slate-200 text-slate-600", hint: "Format non pris en charge" },
  FAILED: { label: "Échec", cls: "bg-red-100 text-red-700", hint: "Réessayez avec « Réanalyser »" },
};

export default function KnowledgePage() {
  const [docs, setDocs] = useState<Doc[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    try {
      setDocs(await apiFetch<Doc[]>("/api/admin/knowledge"));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401)
        setError(e.status === 403 ? "Vous n'avez pas la permission de gérer le guide de l'institut." : e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function upload(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    setInfo(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      if (title.trim()) fd.append("title", title.trim());
      const created = await apiFetch<Doc>("/api/admin/knowledge", { method: "POST", body: fd });
      setDocs((prev) => [created, ...prev]);
      setTitle("");
      if (fileRef.current) fileRef.current.value = "";
      setInfo(`« ${created.title} » ajouté : ${STATUS[created.status].label.toLowerCase()} (${created.chunkCount} extraits).`);
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 400 ? "Fichier refusé : utilisez PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT ou MD." : err.message);
    } finally {
      setUploading(false);
    }
  }

  async function reindex(d: Doc) {
    setError(null);
    setInfo(null);
    try {
      const updated = await apiFetch<Doc>(`/api/admin/knowledge/${d.id}/reindex`, { method: "POST" });
      setDocs((prev) => prev.map((x) => (x.id === d.id ? updated : x)));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function remove(d: Doc) {
    if (!window.confirm(`Supprimer « ${d.title} » ? L'assistant ne l'utilisera plus.`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/admin/knowledge/${d.id}`, { method: "DELETE" });
      setDocs((prev) => prev.filter((x) => x.id !== d.id));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function download(d: Doc) {
    try {
      await downloadFromUrl(`/api/institute/documents/${d.id}/download`, d.title);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Base de connaissances"
        subtitle="Le guide de l'institut : règlement, procédures, calendrier, services, contacts. L'assistant général répond uniquement à partir de ces documents."
      />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <p className="mt-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600">
        Ces documents sont visibles et téléchargeables par tous les utilisateurs connectés : n&apos;y déposez pas
        d&apos;informations confidentielles. Pour modifier le guide, supprimez l&apos;ancien document puis ajoutez le nouveau.
      </p>

      <form onSubmit={upload} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Ajouter un document</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <TextField label="Titre (optionnel)" value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} />
          <label className="block text-sm font-medium text-slate-700">
            Fichier (PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT, MD)
            <input
              ref={fileRef}
              type="file"
              required
              accept=".pdf,.docx,.pptx,.xlsx,.odt,.odp,.txt,.md"
              className="mt-1.5 block w-full text-sm text-slate-700 file:mr-3 file:rounded-lg file:border file:border-slate-200 file:bg-white file:px-3 file:py-2"
            />
          </label>
        </div>
        <button type="submit" disabled={uploading} className={`mt-6 ${primaryBtn}`}>
          {uploading ? "Analyse en cours..." : "Ajouter au guide"}
        </button>
      </form>

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Document</th>
              <th className="px-5 py-3">Type</th>
              <th className="px-5 py-3">Statut</th>
              <th className="px-5 py-3">Ajouté</th>
              <th className="px-5 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Chargement...</td></tr>}
            {!loading && docs.length === 0 && (
              <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucun document : l&apos;assistant général n&apos;a pas encore de source officielle.</td></tr>
            )}
            {docs.map((d) => (
              <tr key={d.id}>
                <td className="px-5 py-4">
                  <span className="font-medium text-slate-900">{d.title}</span>
                  <span className="block text-xs text-slate-500">{formatSize(d.size)}</span>
                </td>
                <td className="px-5 py-4 text-slate-600">{d.type ?? "-"}</td>
                <td className="px-5 py-4">
                  <span title={STATUS[d.status].hint} className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS[d.status].cls}`}>
                    {STATUS[d.status].label}
                  </span>
                  {d.status === "INDEXED" && <span className="ml-2 text-xs text-slate-500">{d.chunkCount} extraits</span>}
                </td>
                <td className="px-5 py-4 text-slate-600">
                  {formatDate(d.uploadedAt)}
                  {d.uploadedBy && <span className="block text-xs text-slate-500">{d.uploadedBy}</span>}
                </td>
                <td className="px-5 py-4 text-right">
                  <div className="flex justify-end gap-2">
                    <button onClick={() => download(d)} className={smallBtn}>Télécharger</button>
                    <button onClick={() => reindex(d)} className={smallBtn}>Réanalyser</button>
                    <button onClick={() => remove(d)} className={`${smallBtn} text-red-600`}>Supprimer</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}