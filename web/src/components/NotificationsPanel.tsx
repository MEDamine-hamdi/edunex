"use client";

import { SectionLoader } from "@/components/Loading";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { downloadFromUrl } from "@/lib/download";
import { formatSize } from "@/lib/format";
import { useMe } from "./RoleShell";
import { ErrorBox, PageHeader, primaryBtn, smallBtn } from "./ui";

type Notif = { id: number; message: string; type: string; read: boolean; sentAt: string };
type Attachment = { id: number; name: string; contentType: string | null; size: number | null; image: boolean };
type Announcement = {
  id: number;
  title: string;
  content: string;
  publishedAt: string;
  className: string | null;
  publishedBy: string;
  attachments: Attachment[];
};

const TYPES: Record<string, string> = {
  NEW_COURSE: "Cours",
  TIMETABLE_CHANGE: "Emploi du temps",
  NEW_GRADE: "Note",
  ATTENDANCE_UPDATE: "Présence",
  ANNOUNCEMENT: "Annonce",
  DOCUMENT_REQUEST: "Demande",
};

const PAGES: Record<string, Record<string, [string, string]>> = {
  ROLE_STUDENT: {
    NEW_COURSE: ["/student/courses", "Voir mes cours"],
    TIMETABLE_CHANGE: ["/student/timetable", "Voir mon emploi du temps"],
    NEW_GRADE: ["/student/grades", "Voir mes notes"],
    ATTENDANCE_UPDATE: ["/student/attendance", "Voir mes absences"],
    DOCUMENT_REQUEST: ["/student/requests", "Voir mes demandes"],
  },
  ROLE_PROFESSOR: {
    TIMETABLE_CHANGE: ["/professor/timetable", "Voir mon emploi du temps"],
    DOCUMENT_REQUEST: ["/professor/requests", "Voir mes demandes"],
  },
  ROLE_ADMIN_AGENT: {
    DOCUMENT_REQUEST: ["/admin/requests", "Traiter les demandes"],
    TIMETABLE_CHANGE: ["/admin/absences", "Voir les absences"],
  },
};

const when = (iso: string) =>
  new Date(iso).toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });

const refreshBell = () => window.dispatchEvent(new Event("edunex:notifications"));

function AttachmentView({ announcementId, a, onError }: { announcementId: number; a: Attachment; onError: (m: string) => void }) {
  const [url, setUrl] = useState<string | null>(null);
  const path = `/api/announcements/${announcementId}/attachments/${a.id}`;

  useEffect(() => {
    if (!a.image) return;
    let created: string | null = null;
    let off = false;
    fetchBlobUrl(path)
      .then((u) => {
        if (off) URL.revokeObjectURL(u);
        else {
          created = u;
          setUrl(u);
        }
      })
      .catch(() => {});
    return () => {
      off = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [a.image, path]);

  const download = () => downloadFromUrl(path, a.name).catch(() => onError("Téléchargement impossible."));

  return (
    <div className="rounded-xl border border-slate-200 p-3">
      {a.image && url && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={url} alt={a.name} className="mb-3 max-h-72 w-full rounded-lg object-contain" />
      )}
      <div className="flex items-center justify-between gap-3 text-sm">
        <span className="min-w-0 truncate font-medium text-slate-800">{a.name}</span>
        <span className="shrink-0 text-xs text-slate-500">{formatSize(a.size)}</span>
        <button onClick={download} className={smallBtn}>Télécharger</button>
      </div>
    </div>
  );
}

type Open = { notif?: Notif; announcement?: Announcement };

export function NotificationsPanel() {
  const me = useMe();
  const [items, setItems] = useState<Notif[]>([]);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [open, setOpen] = useState<Open | null>(null);
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

  function openNotification(n: Notif) {
    markRead(n);
    const announcement = n.type === "ANNOUNCEMENT" ? announcements.find((a) => n.message === `Annonce : ${a.title}`) : undefined;
    setOpen({ notif: n, announcement });
  }

  const unread = items.filter((n) => !n.read).length;
  const target = open?.notif ? PAGES[me.role ?? ""]?.[open.notif.type] : undefined;
  const shown = open?.announcement;

  return (
    <>
      <PageHeader
        title="Notifications"
        subtitle={unread > 0 ? `${unread} non lue${unread > 1 ? "s" : ""}` : "Tout est à jour."}
        action={unread > 0 ? <button onClick={markAll} className={smallBtn}>Tout marquer comme lu</button> : undefined}
      />
      <ErrorBox message={error} />
      {loading && <SectionLoader />}

      <div className="mt-6 grid gap-8 xl:grid-cols-2">
        <section>
          <h2 className="text-lg font-semibold">Mes notifications</h2>
          {!loading && items.length === 0 && (
            <p className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune notification pour le moment.</p>
          )}
          <ul className="mt-4 space-y-3">
            {items.map((n) => (
              <li key={n.id}>
                <button
                  onClick={() => openNotification(n)}
                  className={`w-full rounded-2xl border p-4 text-start shadow-sm transition hover:shadow ${n.read ? "border-slate-200 bg-white" : "border-blue-300 bg-blue-50"}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-bold uppercase text-slate-600">{TYPES[n.type] ?? n.type}</span>
                    <span className="text-xs text-slate-500">{when(n.sentAt)}</span>
                  </div>
                  <p className={`mt-2 line-clamp-2 text-sm ${n.read ? "text-slate-600" : "font-medium text-slate-900"}`}>{n.message}</p>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section>
          <h2 className="text-lg font-semibold">Annonces</h2>
          {!loading && announcements.length === 0 && (
            <p className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune annonce pour le moment.</p>
          )}
          <div className="mt-4 space-y-3">
            {announcements.map((a) => (
              <button key={a.id} onClick={() => setOpen({ announcement: a })} className="block w-full rounded-2xl border border-slate-200 bg-white p-5 text-start shadow-sm transition hover:shadow">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="font-semibold">{a.title}</h3>
                  <span className="shrink-0 text-xs text-slate-500">{when(a.publishedAt)}</span>
                </div>
                <p className="mt-2 line-clamp-2 whitespace-pre-line text-sm text-slate-700">{a.content}</p>
                <p className="mt-3 text-xs text-slate-500">
                  {a.publishedBy}{a.className ? ` · ${a.className}` : ""}{a.attachments.length > 0 ? ` · ${a.attachments.length} pièce(s) jointe(s)` : ""}
                </p>
              </button>
            ))}
          </div>
        </section>
      </div>

      {open && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4" onClick={() => setOpen(null)}>
          <div className="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-bold uppercase text-slate-600">
                  {shown && !open.notif ? "Annonce" : TYPES[open.notif?.type ?? ""] ?? ""}
                </span>
                <p className="mt-2 text-xs text-slate-500">{when(shown && !open.notif ? shown.publishedAt : open.notif!.sentAt)}</p>
              </div>
              <button onClick={() => setOpen(null)} className={smallBtn}>Fermer</button>
            </div>

            {shown ? (
              <>
                <h2 className="mt-4 text-xl font-semibold text-slate-900">{shown.title}</h2>
                <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">{shown.content}</p>
                <p className="mt-3 text-xs text-slate-500">{shown.publishedBy}{shown.className ? ` · ${shown.className}` : ""}</p>
                {shown.attachments.length > 0 && (
                  <div className="mt-5 space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Pièces jointes</p>
                    {shown.attachments.map((a) => (
                      <AttachmentView key={a.id} announcementId={shown.id} a={a} onError={setError} />
                    ))}
                  </div>
                )}
              </>
            ) : (
              <p className="mt-4 whitespace-pre-line text-base leading-relaxed text-slate-800">{open.notif?.message}</p>
            )}

            {target && (
              <Link href={target[0]} className={`mt-6 inline-block ${primaryBtn}`}>{target[1]}</Link>
            )}
          </div>
        </div>
      )}
    </>
  );
}