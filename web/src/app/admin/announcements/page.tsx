"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Audience = "ALL" | "STUDENTS" | "PROFESSORS" | "ADMIN_AGENTS" | "CLASS";
type Announcement = {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
  audience: Audience;
  className: string | null;
  publishedBy: string;
  recipients: number | null;
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
  new Date(iso).toLocaleString("fr-FR", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });

const emptyForm = { title: "", content: "", audience: "ALL" as Audience, classId: "" };

export default function AnnouncementsPage() {
  const [items, setItems] = useState<Announcement[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

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

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setInfo(null);
    try {
      const created = await apiFetch<Announcement>("/api/admin/announcements", {
        method: "POST",
        body: JSON.stringify({
          title: form.title.trim(),
          content: form.content.trim(),
          audience: form.audience,
          classId: form.audience === "CLASS" ? Number(form.classId) : null,
        }),
      });
      setItems((prev) => [created, ...prev]);
      setInfo(`Annonce publiée : ${created.recipients ?? 0} destinataire(s) notifié(s).`);
      setForm(emptyForm);
      setShowForm(false);
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Annonces"
        subtitle="Publiez une annonce : chaque destinataire reçoit une notification."
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
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Publication..." : "Publier l'annonce"}
          </button>
        </form>
      )}

      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}
      {!loading && items.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune annonce publiée.</p>
      )}
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
            <p className="mt-3 text-xs text-slate-500">{a.publishedBy} · {when(a.publishedAt)}</p>
          </article>
        ))}
      </div>
    </AdminShell>
  );
}