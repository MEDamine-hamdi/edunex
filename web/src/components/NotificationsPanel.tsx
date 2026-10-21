"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, PageHeader, smallBtn } from "./ui";

type Notif = { id: number; message: string; type: string; read: boolean; sentAt: string };
type Announcement = {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
  className: string | null;
  publishedBy: string;
};

const TYPES: Record<string, string> = {
  NEW_COURSE: "Cours",
  TIMETABLE_CHANGE: "Emploi du temps",
  NEW_GRADE: "Note",
  ATTENDANCE_UPDATE: "Présence",
  ANNOUNCEMENT: "Annonce",
  DOCUMENT_REQUEST: "Demande",
};

const when = (iso: string) =>
  new Date(iso).toLocaleString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });

const refreshBell = () => window.dispatchEvent(new Event("edunex:notifications"));

export function NotificationsPanel() {
  const [items, setItems] = useState<Notif[]>([]);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [n, a] = await Promise.all([
        apiFetch<Notif[]>("/api/notifications"),
        apiFetch<Announcement[]>("/api/announcements"),
      ]);
      setItems(n);
      setAnnouncements(a);
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

  async function markRead(n: Notif) {
    if (n.read) return;
    try {
      await apiFetch<Notif>(`/api/notifications/${n.id}/read`, { method: "PUT" });
      setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      refreshBell();
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  }

  async function markAll() {
    try {
      await apiFetch<void>("/api/notifications/read-all", { method: "PUT" });
      setItems((prev) => prev.map((x) => ({ ...x, read: true })));
      refreshBell();
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  }

  const unread = items.filter((n) => !n.read).length;

  return (
    <>
      <PageHeader
        title="Notifications"
        subtitle={unread > 0 ? `${unread} non lue${unread > 1 ? "s" : ""}` : "Tout est à jour."}
        action={
          unread > 0 ? (
            <button onClick={markAll} className={smallBtn}>Tout marquer comme lu</button>
          ) : undefined
        }
      />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}

      <div className="mt-6 grid gap-8 xl:grid-cols-2">
        <section>
          <h2 className="text-lg font-semibold">Mes notifications</h2>
          {!loading && items.length === 0 && (
            <p className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
              Aucune notification pour le moment.
            </p>
          )}
          <ul className="mt-4 space-y-3">
            {items.map((n) => (
              <li key={n.id}>
                <button
                  onClick={() => markRead(n)}
                  className={`w-full rounded-2xl border p-4 text-left shadow-sm ${
                    n.read ? "border-slate-200 bg-white" : "border-blue-300 bg-blue-50"
                  }`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-bold uppercase text-slate-600">
                      {TYPES[n.type] ?? n.type}
                    </span>
                    <span className="text-xs text-slate-500">{when(n.sentAt)}</span>
                  </div>
                  <p className={`mt-2 text-sm ${n.read ? "text-slate-600" : "font-medium text-slate-900"}`}>{n.message}</p>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h2 className="text-lg font-semibold">Annonces</h2>
          {!loading && announcements.length === 0 && (
            <p className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
              Aucune annonce pour le moment.
            </p>
          )}
          <div className="mt-4 space-y-3">
            {announcements.map((a) => (
              <article key={a.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="font-semibold">{a.title}</h3>
                  <span className="shrink-0 text-xs text-slate-500">{when(a.publishedAt)}</span>
                </div>
                <p className="mt-2 whitespace-pre-line text-sm text-slate-700">{a.content}</p>
                <p className="mt-3 text-xs text-slate-500">
                  {a.publishedBy}{a.className ? ` · ${a.className}` : ""}
                </p>
              </article>
            ))}
          </div>
        </section>
      </div>
    </>
  );
}