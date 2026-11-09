"use client";

import { KeyboardEvent, useCallback, useEffect, useRef, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { downloadFromUrl } from "@/lib/download";
import { useMe } from "./RoleShell";
import { UserAvatar, forgetAvatar } from "./UserAvatar";
import { ErrorBox, inputCls, primaryBtn, smallBtn } from "./ui";

type Contact = { id: number; name: string; role: string };
type ConvRow = { id: number; type: "DIRECT" | "GROUP"; title: string; photoUserId: number | null; hasPhoto: boolean; lastText: string | null; lastAt: string | null; unread: number; members: number };
type Member = { id: number; name: string; role: string; admin: boolean };
type ConvDetail = { id: number; type: "DIRECT" | "GROUP"; title: string; photoUserId: number | null; hasPhoto: boolean; iAmAdmin: boolean; members: Member[] };
type Msg = { id: number; senderId: number; senderName: string; content: string | null; createdAt: string; deleted: boolean; deletedByOther: boolean; hasFile: boolean; fileName: string | null; fileKind: "IMAGE" | "DOCUMENT" | null; fileSize: number | null };
type Page = { messages: Msg[]; serverTime: string };

const photoPath = (c: { type: string; id: number; photoUserId: number | null; hasPhoto: boolean }, v: number) =>
  c.type === "DIRECT" ? (c.photoUserId ? `/api/messaging/users/${c.photoUserId}/photo` : null) : c.hasPhoto ? `/api/messaging/conversations/${c.id}/photo?v=${v}` : null;

const clock = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
const day = (iso: string) => new Date(iso).toLocaleDateString(undefined, { day: "2-digit", month: "short" });

function merge(prev: Msg[], incoming: Msg[]): Msg[] {
  if (incoming.length === 0) return prev;
  const map = new Map(prev.map((m) => [m.id, m] as const));
  incoming.forEach((m) => map.set(m.id, m));
  return Array.from(map.values()).sort((a, b) => a.id - b.id);
}

function errorText(e: unknown): string {
  if (!(e instanceof ApiError)) return "Une erreur est survenue.";
  if (e.status === 429) return "Vous envoyez trop de messages. Patientez un instant.";
  if (e.status === 400) return "Message refusé : seuls les images et les documents (10 Mo maximum) sont acceptés.";
  if (e.status === 403) return "Action non autorisée.";
  return e.message;
}

function ImageMessage({ id }: { id: number }) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    let created: string | null = null;
    let off = false;
    fetchBlobUrl(`/api/messaging/messages/${id}/file`).then((u) => {
      if (off) URL.revokeObjectURL(u);
      else {
        created = u;
        setUrl(u);
      }
    }).catch(() => {});
    return () => {
      off = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [id]);
  // eslint-disable-next-line @next/next/no-img-element
  return url ? <img src={url} alt="" className="max-h-64 rounded-xl object-contain" /> : <span className="text-xs opacity-70">Chargement de l&apos;image...</span>;
}

function NewConversation({ onClose, onOpen }: { onClose: () => void; onOpen: (id: number) => void }) {
  const [tab, setTab] = useState<"direct" | "group">("direct");
  const [q, setQ] = useState("");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [picked, setPicked] = useState<number[]>([]);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => apiFetch<Contact[]>(`/api/messaging/contacts?q=${encodeURIComponent(q)}`).then(setContacts).catch((e) => setError(errorText(e))), 200);
    return () => clearTimeout(t);
  }, [q]);

  async function direct(id: number) {
    try {
      const d = await apiFetch<ConvDetail>("/api/messaging/conversations/direct", { method: "POST", body: JSON.stringify({ userId: id }) });
      onOpen(d.id);
    } catch (e) {
      setError(errorText(e));
    }
  }

  async function createGroup() {
    try {
      const d = await apiFetch<ConvDetail>("/api/messaging/conversations/group", { method: "POST", body: JSON.stringify({ name: name.trim(), memberIds: picked }) });
      onOpen(d.id);
    } catch (e) {
      setError(errorText(e));
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
      <div className="flex max-h-[85vh] w-full max-w-md flex-col rounded-2xl bg-white p-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Nouvelle conversation</h2>
          <button onClick={onClose} className={smallBtn}>Fermer</button>
        </div>
        <div className="mt-3 flex gap-2">
          <button onClick={() => setTab("direct")} className={`${smallBtn} ${tab === "direct" ? "border-blue-400 text-blue-700" : ""}`}>Message privé</button>
          <button onClick={() => setTab("group")} className={`${smallBtn} ${tab === "group" ? "border-blue-400 text-blue-700" : ""}`}>Groupe</button>
        </div>
        <ErrorBox message={error} />
        {tab === "group" && <input value={name} maxLength={100} onChange={(e) => setName(e.target.value)} placeholder="Nom du groupe" className={inputCls} />}
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher une personne" className={inputCls} />
        <ul className="mt-3 min-h-0 flex-1 space-y-1 overflow-y-auto">
          {contacts.length === 0 && <li className="px-2 py-3 text-sm text-slate-500">Aucune personne trouvée.</li>}
          {contacts.map((c) => (
            <li key={c.id}>
              <button
                onClick={() => (tab === "direct" ? direct(c.id) : setPicked((p) => (p.includes(c.id) ? p.filter((x) => x !== c.id) : [...p, c.id])))}
                className={`flex w-full items-center gap-3 rounded-xl px-3 py-2 text-start hover:bg-slate-50 ${picked.includes(c.id) ? "bg-blue-50" : ""}`}
              >
                <UserAvatar path={null} name={c.name} size={36} />
                <span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium text-slate-900">{c.name}</span><span className="text-xs text-slate-500">{c.role}</span></span>
                {tab === "group" && picked.includes(c.id) && <span className="text-blue-600">✓</span>}
              </button>
            </li>
          ))}
        </ul>
        {tab === "group" && (
          <button onClick={createGroup} disabled={!name.trim() || picked.length === 0} className={`mt-4 ${primaryBtn}`}>Créer le groupe ({picked.length})</button>
        )}
      </div>
    </div>
  );
}

function GroupInfo({ detail, version, onClose, onChanged }: { detail: ConvDetail; version: number; onClose: () => void; onChanged: (leave?: boolean) => void }) {
  const me = useMe();
  const [name, setName] = useState(detail.title);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [q, setQ] = useState("");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const file = useRef<HTMLInputElement>(null);
  const base = `/api/messaging/conversations/${detail.id}`;

  useEffect(() => {
    if (!adding) return;
    const t = setTimeout(() => apiFetch<Contact[]>(`/api/messaging/contacts?q=${encodeURIComponent(q)}`).then(setContacts).catch(() => {}), 200);
    return () => clearTimeout(t);
  }, [adding, q]);

  async function run(fn: () => Promise<unknown>, leave = false) {
    setError(null);
    try {
      await fn();
      onChanged(leave);
    } catch (e) {
      setError(errorText(e));
    }
  }

  const memberIds = new Set(detail.members.map((m) => m.id));

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
      <div className="max-h-[88vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-5 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Le groupe</h2>
          <button onClick={onClose} className={smallBtn}>Fermer</button>
        </div>
        <ErrorBox message={error} />
        <div className="mt-4 flex items-center gap-4">
          <UserAvatar path={detail.hasPhoto ? `/api/messaging/conversations/${detail.id}/photo?v=${version}` : null} name={detail.title} size={64} />
          {detail.iAmAdmin && (
            <div className="flex flex-wrap gap-2">
              <input ref={file} type="file" hidden accept=".png,.jpg,.jpeg,.webp" onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) { const fd = new FormData(); fd.append("file", f); run(() => apiFetch(`${base}/photo`, { method: "PUT", body: fd })); }
                e.target.value = "";
              }} />
              <button onClick={() => file.current?.click()} className={smallBtn}>Changer l&apos;image</button>
              {detail.hasPhoto && <button onClick={() => run(() => apiFetch(`${base}/photo`, { method: "DELETE" }))} className={smallBtn}>Retirer l&apos;image</button>}
            </div>
          )}
        </div>
        {detail.iAmAdmin && (
          <div className="mt-4 flex gap-2">
            <input value={name} maxLength={100} onChange={(e) => setName(e.target.value)} className={`${inputCls} mt-0`} />
            <button onClick={() => run(() => apiFetch(base, { method: "PUT", body: JSON.stringify({ name: name.trim() }) }))} disabled={!name.trim() || name.trim() === detail.title} className={primaryBtn}>Enregistrer</button>
          </div>
        )}
        <h3 className="mt-6 text-sm font-semibold text-slate-900">Membres ({detail.members.length})</h3>
        <ul className="mt-2 space-y-1">
          {detail.members.map((m) => (
            <li key={m.id} className="flex items-center gap-3 rounded-xl px-2 py-1.5">
              <UserAvatar path={`/api/messaging/users/${m.id}/photo`} name={m.name} size={32} />
              <span className="min-w-0 flex-1 text-sm"><span className="block truncate font-medium text-slate-900">{m.name}{m.id === me.id ? " (vous)" : ""}</span><span className="text-xs text-slate-500">{m.admin ? "Administrateur" : m.role}</span></span>
              {detail.iAmAdmin && m.id !== me.id && <button onClick={() => run(() => apiFetch(`${base}/members/${m.id}`, { method: "DELETE" }))} className={`${smallBtn} text-red-600`}>Retirer</button>}
            </li>
          ))}
        </ul>
        {detail.iAmAdmin && (
          <div className="mt-3">
            <button onClick={() => setAdding((v) => !v)} className={smallBtn}>{adding ? "Fermer" : "Ajouter des membres"}</button>
            {adding && (
              <div className="mt-3">
                <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher" className={`${inputCls} mt-0`} />
                <ul className="mt-2 max-h-48 space-y-1 overflow-y-auto">
                  {contacts.filter((c) => !memberIds.has(c.id)).map((c) => (
                    <li key={c.id}><button onClick={() => run(() => apiFetch(`${base}/members`, { method: "POST", body: JSON.stringify({ userIds: [c.id] }) }))} className="flex w-full items-center justify-between rounded-xl px-3 py-2 text-start text-sm hover:bg-slate-50"><span>{c.name} <span className="text-xs text-slate-500">{c.role}</span></span><span className="text-blue-600">Ajouter</span></button></li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
        <button onClick={() => window.confirm("Quitter ce groupe ?") && run(() => apiFetch(`${base}/members/${me.id}`, { method: "DELETE" }), true)} className={`mt-6 ${smallBtn} text-red-600`}>Quitter le groupe</button>
      </div>
    </div>
  );
}

export function MessagingApp() {
  const me = useMe();
  const [rows, setRows] = useState<ConvRow[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ConvDetail | null>(null);
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showNew, setShowNew] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [version, setVersion] = useState(0);
  const serverTime = useRef<string | null>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const meId = me.id;

  const loadRows = useCallback(() => {
    apiFetch<ConvRow[]>("/api/messaging/conversations").then(setRows).catch(() => {});
  }, []);

  useEffect(() => {
    loadRows();
    const t = setInterval(() => !document.hidden && loadRows(), 6000);
    return () => clearInterval(t);
  }, [loadRows]);

  const markRead = useCallback((id: number) => {
    apiFetch(`/api/messaging/conversations/${id}/read`, { method: "POST" }).then(loadRows).catch(() => {});
  }, [loadRows]);

  const loadDetail = useCallback((id: number) => {
    apiFetch<ConvDetail>(`/api/messaging/conversations/${id}`).then(setDetail).catch(() => {});
  }, []);

  // the open conversation refreshes every 2 seconds (new messages and deletions)
  useEffect(() => {
    if (!activeId) return;
    let stop = false;
    setMessages([]);
    setDetail(null);
    serverTime.current = null;
    apiFetch<Page>(`/api/messaging/conversations/${activeId}/messages`)
      .then((p) => {
        if (stop) return;
        setMessages(p.messages);
        serverTime.current = p.serverTime;
        markRead(activeId);
      })
      .catch((e) => setError(errorText(e)));
    loadDetail(activeId);
    const timer = setInterval(async () => {
      if (document.hidden || !serverTime.current) return;
      try {
        const p = await apiFetch<Page>(`/api/messaging/conversations/${activeId}/messages?since=${encodeURIComponent(serverTime.current)}`);
        if (stop) return;
        serverTime.current = p.serverTime;
        if (p.messages.length > 0) {
          setMessages((prev) => merge(prev, p.messages));
          if (p.messages.some((m) => m.senderId !== meId && !m.deleted)) markRead(activeId);
        }
      } catch {
        /* transient error: the next refresh retries */
      }
    }, 2000);
    return () => {
      stop = true;
      clearInterval(timer);
    };
  }, [activeId, meId, markRead, loadDetail]);

  useEffect(() => {
    const el = listRef.current;
    if (el && (el.scrollHeight - el.scrollTop - el.clientHeight < 240 || messages[messages.length - 1]?.senderId === meId)) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages, meId]);

  async function send() {
    const text = input.trim();
    if ((!text && !file) || !activeId || sending) return;
    setSending(true);
    setError(null);
    try {
      let created: Msg;
      if (file) {
        const fd = new FormData();
        if (text) fd.append("content", text);
        fd.append("file", file);
        created = await apiFetch<Msg>(`/api/messaging/conversations/${activeId}/messages/file`, { method: "POST", body: fd });
      } else {
        created = await apiFetch<Msg>(`/api/messaging/conversations/${activeId}/messages`, { method: "POST", body: JSON.stringify({ content: text }) });
      }
      setMessages((prev) => merge(prev, [created]));
      setInput("");
      setFile(null);
      loadRows();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setSending(false);
    }
  }

  async function remove(m: Msg) {
    if (!window.confirm("Supprimer ce message ? Les autres participants verront qu'un message a été supprimé.")) return;
    try {
      const updated = await apiFetch<Msg>(`/api/messaging/messages/${m.id}`, { method: "DELETE" });
      setMessages((prev) => merge(prev, [updated]));
      loadRows();
    } catch (e) {
      setError(errorText(e));
    }
  }

  function onKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  const active = rows.find((r) => r.id === activeId);
  const isGroup = (detail?.type ?? active?.type) === "GROUP";

  return (
    <div className="grid h-[calc(100dvh-9rem)] min-h-[28rem] gap-4 lg:grid-cols-[320px_1fr]">
      <aside className="flex min-h-0 flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 p-3">
          <h2 className="font-semibold text-slate-900">Messagerie</h2>
          <button onClick={() => setShowNew(true)} className={primaryBtn}>+ Nouvelle</button>
        </div>
        <ul className="min-h-0 flex-1 overflow-y-auto p-2">
          {rows.length === 0 && <li className="px-3 py-6 text-center text-sm text-slate-500">Aucune conversation. Commencez-en une avec « + Nouvelle ».</li>}
          {rows.map((r) => (
            <li key={r.id}>
              <button onClick={() => setActiveId(r.id)} className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-start ${r.id === activeId ? "bg-blue-50" : "hover:bg-slate-50"}`}>
                <UserAvatar path={photoPath(r, r.id === activeId ? version : 0)} name={r.title} size={42} />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-2"><span className="truncate text-sm font-semibold text-slate-900">{r.title}</span>{r.lastAt && <span className="shrink-0 text-[11px] text-slate-400">{day(r.lastAt)}</span>}</span>
                  <span className="block truncate text-xs text-slate-500">{r.lastText ?? "Aucun message"}</span>
                </span>
                {r.unread > 0 && <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-blue-600 px-1 text-[11px] font-bold text-white">{r.unread}</span>}
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <section className="flex min-h-0 flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">
        {!activeId ? (
          <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-slate-500">Choisissez une conversation ou créez-en une.</div>
        ) : (
          <>
            <header className="flex items-center gap-3 border-b border-slate-200 p-3">
              <UserAvatar path={active ? photoPath(active, version) : null} name={detail?.title ?? active?.title ?? ""} size={40} />
              <div className="min-w-0 flex-1">
                <p className="truncate font-semibold text-slate-900">{detail?.title ?? active?.title}</p>
                <p className="text-xs text-slate-500">{isGroup ? `${detail?.members.length ?? active?.members ?? ""} membres` : "Conversation privée"}</p>
              </div>
              {isGroup && detail && <button onClick={() => setShowInfo(true)} className={smallBtn}>Infos du groupe</button>}
            </header>
            <div ref={listRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
              {messages.map((m) => {
                const mine = m.senderId === meId;
                return (
                  <div key={m.id} className={`group flex items-end gap-2 ${mine ? "flex-row-reverse" : ""}`}>
                    {!mine && <UserAvatar path={`/api/messaging/users/${m.senderId}/photo`} name={m.senderName} size={30} />}
                    <div className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-[15px] ${m.deleted ? "border border-dashed border-slate-300 text-slate-500" : mine ? "bg-blue-600 text-white" : "border border-slate-200 bg-white text-slate-800"}`}>
                      {!mine && isGroup && <p className="mb-0.5 text-xs font-semibold opacity-70">{m.senderName}</p>}
                      {m.deleted ? (
                        <p className="text-sm italic">{m.deletedByOther ? "Ce message a été supprimé par un administrateur du groupe" : "Ce message a été supprimé"}</p>
                      ) : (
                        <>
                          {m.hasFile && m.fileKind === "IMAGE" && <ImageMessage id={m.id} />}
                          {m.hasFile && m.fileKind === "DOCUMENT" && (
                            <button onClick={() => downloadFromUrl(`/api/messaging/messages/${m.id}/file`, m.fileName ?? "document").catch(() => setError("Téléchargement impossible."))} className={`mb-1 block max-w-full truncate rounded-lg px-2 py-1 text-start text-sm underline ${mine ? "bg-white/20" : "bg-slate-100"}`}>{m.fileName}</button>
                          )}
                          {m.content && <p className="whitespace-pre-wrap break-words">{m.content}</p>}
                        </>
                      )}
                      <p className={`mt-1 text-end text-[10px] ${mine && !m.deleted ? "text-blue-100" : "text-slate-400"}`}>{clock(m.createdAt)}</p>
                    </div>
                    {!m.deleted && (mine || (isGroup && detail?.iAmAdmin)) && (
                      <button onClick={() => remove(m)} aria-label="Supprimer le message" title="Supprimer" className="invisible rounded-lg px-2 py-1 text-xs text-slate-400 hover:bg-red-50 hover:text-red-600 group-hover:visible">Supprimer</button>
                    )}
                  </div>
                );
              })}
            </div>
            <div className="border-t border-slate-200 p-3">
              <ErrorBox message={error} />
              {file && <p className="mb-2 text-xs text-blue-700">Fichier : {file.name} <button onClick={() => setFile(null)} className="ms-1 text-slate-400 hover:text-red-600">✕</button></p>}
              <div className="flex items-end gap-2">
                <input ref={fileInput} type="file" hidden accept=".png,.jpg,.jpeg,.webp,.gif,.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.odt,.odp,.txt,.csv" onChange={(e) => { setFile(e.target.files?.[0] ?? null); e.target.value = ""; }} />
                <button onClick={() => fileInput.current?.click()} disabled={sending} className={`${smallBtn} py-2.5`} title="Image ou document, 10 Mo maximum">Joindre</button>
                <textarea value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={onKey} rows={1} maxLength={4000} placeholder="Écrire un message…" className="min-w-0 flex-1 resize-none rounded-xl border border-slate-200 px-4 py-2.5 text-[15px] focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100" />
                <button onClick={send} disabled={sending || (!input.trim() && !file)} className={primaryBtn}>Envoyer</button>
              </div>
            </div>
          </>
        )}
      </section>

      {showNew && <NewConversation onClose={() => setShowNew(false)} onOpen={(id) => { setShowNew(false); setActiveId(id); loadRows(); }} />}
      {showInfo && detail && (
        <GroupInfo
          detail={detail}
          version={version}
          onClose={() => setShowInfo(false)}
          onChanged={(leave) => {
            if (leave) { setShowInfo(false); setActiveId(null); } else { forgetAvatar(`/api/messaging/conversations/${detail.id}/photo?v=${version}`); setVersion((v) => v + 1); loadDetail(detail.id); }
            loadRows();
          }}
        />
      )}
    </div>
  );
}