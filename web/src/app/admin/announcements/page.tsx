"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { downloadFromUrl } from "@/lib/download";

type Audience = "ALL" | "STUDENTS" | "PROFESSORS" | "ADMIN_AGENTS" | "CLASS";
type Attachment = { id: number; name: string; image: boolean };
type Announcement = {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
  audience: Audience;
  className: string | null;
  publishedBy: string;
  recipients: number | null;
  attachments: Attachment[];
};
type ClassItem = { id: number; name: string; academicYear: string };

const AUDIENCES: Record<Audience, string> = {
  ALL: "Tout le monde",
  STUDENTS: "Étudiants",
  PROFESSORS: "Professeurs",
  ADMIN_AGENTS: "Agents d'administration",
  CLASS: "Une classe",
};

const when = (iso: string) =>
  new Date(iso).toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });

const emptyForm = { title: "", content: "", audience: "ALL" as Audience, classId: "" };

export default function AnnouncementsPage() {
  const [items, setItems] = useState<Announcement[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [files, setFiles] = useState<File[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const fileRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    try {
      const [a, c] = await Promise.all([
        apiFetch<Announcement[]>("/api/admin/announcements"),
        apiFetch<ClassItem[]>("/api/admin/classes"),
      ]);
      setItems(a);
      setClasses(c);
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

  function pick(list: FileList | null) {
    if (!list) return;
    const next = [...files, ...Array.from(list)].slice(0, 5);
    setFiles(next);
    if (fileRef.current) fileRef.current.value = "";
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setInfo(null);
    const data = {
      title: form.title.trim(),
      content: form.content.trim(),
      audience: form.audience,
      classId: form.audience === "CLASS" ? Number(form.classId) : null,
    };
    try {
      let created: Announcement;
      if (files.length > 0) {
        const fd = new FormData();
        fd.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
        files.forEach((f) => fd.append("files", f));
        created = await apiFetch<Announcement>("/api/admin/announcements/with-files", { method: "POST", body: fd });
      } else {
        created = await apiFetch<Announcement>("/api/admin/announcements", { method: "POST", body: JSON.stringify(data) });
      }
      setItems((prev) => [created, ...prev]);
      setInfo(`Annonce publiée : ${created.recipients ?? 0} destinataire(s) notifié(s).`);
      setForm(emptyForm);
      setFiles([]);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 400 ? "Vérifiez le formulaire et les fichiers (5 maximum, 10 Mo chacun : images, PDF, Word, PowerPoint, Excel, texte)." : err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Annonces"
        subtitle="Publiez une annonce, avec des images ou des documents : chaque destinataire reçoit une notification."
        action={
          <button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>
            {showForm ? "Fermer" : "+ Nouvelle annonce"}
          </button>
        }
      />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {showForm && (
        <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Titre" maxLength={200} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            <label className="block text-sm font-medium text-slate-700">
              Destinataires
              <select value={form.audience} onChange={(e) => setForm({ ...form, audience: e.target.value as Audience })} className={inputCls}>
                {(Object.keys(AUDIENCES) as Audience[]).map((a) => (
                  <option key={a} value={a}>{AUDIENCES[a]}</option>
                ))}
              </select>
            </label>
            {form.audience === "CLASS" && (
              <label className="block text-sm font-medium text-slate-700">
                Classe
                <select required value={form.classId} onChange={(e) => setForm({ ...form, classId: e.target.value })} className={inputCls}>
                  <option value="">Choisir...</option>
                  {classes.map((c) => (
                    <option key={c.id} value={c.id}>{c.name} ({c.academicYear})</option>
                  ))}
                </select>
              </label>
            )}
            <label className="block text-sm font-medium text-slate-700 sm:col-span-2">
              Message
              <textarea rows={4} required value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} className={inputCls} />
            </label>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">
                Pièces jointes (images ou documents, 5 maximum, 10 Mo chacune)
                <input ref={fileRef} type="file" multiple accept=".png,.jpg,.jpeg,.webp,.gif,.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.odt,.odp,.txt,.csv"
                  onChange={(e) => pick(e.target.files)}
                  className="mt-1.5 block w-full text-sm text-slate-700 file:mr-3 file:rounded-lg file:border file:border-slate-200 file:bg-white file:px-3 file:py-2" />
              </label>
              {files.length > 0 && (
                <ul className="mt-3 flex flex-wrap gap-2">
                  {files.map((f, i) => (
                    <li key={`${f.name}-${i}`} className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-1.5 text-xs text-slate-700">
                      {f.name}
                      <button type="button" aria-label="Retirer" onClick={() => setFiles(files.filter((_, j) => j !== i))} className="text-slate-400 hover:text-red-600">✕</button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>{saving ? "Publication..." : "Publier l'annonce"}</button>
        </form>
      )}

      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && items.length === 0 && <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune annonce publiée.</p>}
      <div className="mt-6 space-y-4">
        {items.map((a) => (
          <article key={a.id} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold">{a.title}</h2>
              <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700">
                {AUDIENCES[a.audience]}{a.className ? ` : ${a.className}` : ""}
              </span>
            </div>
            <p className="mt-2 whitespace-pre-line text-sm text-slate-700">{a.content}</p>
            {a.attachments.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-2">
                {a.attachments.map((f) => (
                  <button key={f.id} onClick={() => downloadFromUrl(`/api/announcements/${a.id}/attachments/${f.id}`, f.name).catch(() => setError("Téléchargement impossible."))} className={smallBtn}>
                    {f.name}
                  </button>
                ))}
              </div>
            )}
            <p className="mt-3 text-xs text-slate-500">{a.publishedBy} · {when(a.publishedAt)}</p>
          </article>
        ))}
      </div>
    </AdminShell>
  );
}