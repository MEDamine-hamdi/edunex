"use client";

import { Fragment, KeyboardEvent, ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { useMe } from "@/components/RoleShell";
import { ErrorBox, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch, downloadDocument } from "@/lib/api";
import { downloadFromUrl } from "@/lib/download";

/** A conversation tied to a subject (and optionally one of its courses). Without scope: the general assistant. */
export type ChatScope = { subjectId: number; courseId?: number | null; label: string };

type Source = {
  n: number;
  documentId: number;
  documentTitle: string;
  courseTitle: string;
  subjectName: string;
  kind: "COURSE" | "INSTITUTE";
};
type Msg = { id: number; role: "USER" | "AI"; content: string; createdAt: string; sources: Source[] | null };
type SessionItem = { id: number; title: string; createdAt: string; updatedAt: string };
type Reply = { sessionId: number; title: string; userMessage: Msg; assistantMessage: Msg };
type ScopeInfo = { kind: "GENERAL" | "SUBJECT" | "COURSE"; label: string; documents: number; indexedDocuments: number; available: boolean };

const GENERAL_SUGGESTIONS = [
  "Dans quel délai dois-je justifier une absence ?",
  "Comment fonctionne l'inscription aux examens ?",
  "Quels sont les horaires et les contacts du secrétariat ?",
];
const COURSE_SUGGESTIONS = [
  "Résume-moi les points essentiels de ce cours.",
  "Explique-moi les notions clés avec un exemple.",
  "Pose-moi 3 questions pour vérifier ma compréhension.",
];

function scopeQuery(scope?: ChatScope): string {
  if (!scope) return "";
  const p = new URLSearchParams({ subjectId: String(scope.subjectId) });
  if (scope.courseId) p.set("courseId", String(scope.courseId));
  return `?${p.toString()}`;
}

function friendly(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 429) return "Vous envoyez trop de messages. Patientez quelques secondes puis réessayez.";
    if (e.status === 503) return "L'assistant est momentanément indisponible. Réessayez dans un instant.";
    if (e.status === 403) return "Vous n'avez pas (ou plus) accès à ces supports.";
    return e.message;
  }
  return "Une erreur est survenue.";
}

function inline(text: string): ReactNode[] {
  return text.split(/(\*\*[^*\n]+\*\*|`[^`\n]+`)/g).map((part, i) => {
    if (part.length > 4 && part.startsWith("**") && part.endsWith("**")) return <strong key={i}>{part.slice(2, -2)}</strong>;
    if (part.length > 2 && part.startsWith("`") && part.endsWith("`"))
      return (
        <code key={i} className="rounded bg-slate-100 px-1 py-0.5 text-[13px] text-slate-800">
          {part.slice(1, -1)}
        </code>
      );
    return <Fragment key={i}>{part}</Fragment>;
  });
}

/** Small safe renderer: paragraphs, lists, **bold**, `code` and code blocks. */
function RichText({ text }: { text: string }) {
  const blocks: ReactNode[] = [];
  let para: string[] = [];
  let items: string[] = [];
  let ordered = false;
  let code: string[] | null = null;

  const flushPara = () => {
    if (para.length) {
      blocks.push(<p key={blocks.length} className="whitespace-pre-line">{inline(para.join("\n"))}</p>);
      para = [];
    }
  };
  const flushList = () => {
    if (!items.length) return;
    const lis = items.map((it, i) => <li key={i}>{inline(it)}</li>);
    blocks.push(
      ordered ? (
        <ol key={blocks.length} className="ml-5 list-decimal space-y-1">{lis}</ol>
      ) : (
        <ul key={blocks.length} className="ml-5 list-disc space-y-1">{lis}</ul>
      ),
    );
    items = [];
  };
  const pre = (lines: string[]) => (
    <pre key={blocks.length} className="overflow-x-auto rounded-lg bg-slate-900 p-3 text-[13px] text-slate-100">
      {lines.join("\n")}
    </pre>
  );

  for (const line of text.split("\n")) {
    if (line.trim().startsWith("```")) {
      if (code) {
        blocks.push(pre(code));
        code = null;
      } else {
        flushPara();
        flushList();
        code = [];
      }
      continue;
    }
    if (code) {
      code.push(line);
      continue;
    }
    const bullet = /^\s*[-*•]\s+(.*)$/.exec(line);
    const number = /^\s*\d+[.)]\s+(.*)$/.exec(line);
    if (bullet || number) {
      const isOrdered = !!number;
      if (items.length && isOrdered !== ordered) flushList();
      flushPara();
      ordered = isOrdered;
      items.push(((bullet ?? number) as RegExpExecArray)[1]);
    } else if (!line.trim()) {
      flushPara();
      flushList();
    } else {
      flushList();
      para.push(line);
    }
  }
  if (code) blocks.push(pre(code));
  flushPara();
  flushList();
  return <div className="space-y-2">{blocks}</div>;
}

export function AssistantChat({ scope, compact = false }: { scope?: ChatScope; compact?: boolean }) {
  const me = useMe();
  const [sessions, setSessions] = useState<SessionItem[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<Msg[]>([]);
  const [info, setInfo] = useState<ScopeInfo | null>(null);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const query = scopeQuery(scope);

  const loadSessions = useCallback(async () => {
    try {
      setSessions(await apiFetch<SessionItem[]>(`/api/assistant/sessions${query}`));
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) setError(friendly(e));
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    loadSessions();
    apiFetch<ScopeInfo>(`/api/assistant/scope-info${query}`)
      .then(setInfo)
      .catch((e) => {
        if (!(e instanceof ApiError && e.status === 401)) setError(friendly(e));
      });
  }, [loadSessions, query]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, sending]);

  async function openSession(id: number) {
    if (sending) return;
    setError(null);
    setActiveId(id);
    try {
      setMessages(await apiFetch<Msg[]>(`/api/assistant/sessions/${id}/messages`));
    } catch (e) {
      setError(friendly(e));
    }
  }

  function newChat() {
    if (sending) return;
    setActiveId(null);
    setMessages([]);
    setError(null);
  }

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    const tempId = -Date.now();
    setSending(true);
    setError(null);
    setInput("");
    setMessages((prev) => [
      ...prev,
      { id: tempId, role: "USER", content: text, createdAt: new Date().toISOString(), sources: null },
    ]);
    try {
      const reply = await apiFetch<Reply>("/api/assistant/chat", {
        method: "POST",
        body: JSON.stringify({
          sessionId: activeId,
          subjectId: scope?.subjectId ?? null,
          courseId: scope?.courseId ?? null,
          message: text,
        }),
      });
      setActiveId(reply.sessionId);
      setMessages((prev) => [...prev.filter((m) => m.id !== tempId), reply.userMessage, reply.assistantMessage]);
      loadSessions();
    } catch (e) {
      setMessages((prev) => prev.filter((m) => m.id !== tempId));
      setInput(text);
      setError(friendly(e));
    } finally {
      setSending(false);
    }
  }

  async function remove(id: number) {
    if (!window.confirm("Supprimer cette conversation ?")) return;
    try {
      await apiFetch<void>(`/api/assistant/sessions/${id}`, { method: "DELETE" });
      if (id === activeId) {
        setActiveId(null);
        setMessages([]);
      }
      await loadSessions();
    } catch (e) {
      setError(friendly(e));
    }
  }

  async function openSource(s: Source) {
    try {
      if (s.kind === "INSTITUTE") {
        const info = await apiFetch<{ kind: string; url: string | null }>(`/api/institute/documents/${s.documentId}/info`);
        if (info.kind === "LINK" && info.url) window.open(info.url, "_blank", "noopener,noreferrer");
        else await downloadFromUrl(`/api/institute/documents/${s.documentId}/download`, s.documentTitle);
      }
      else await downloadDocument(s.documentId, s.documentTitle);
    } catch (e) {
      setError(friendly(e));
    }
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  let notice: string | null = null;
  if (info) {
    if (!info.available) notice = "L'assistant est désactivé ou n'est pas encore configuré.";
    else if (info.kind === "GENERAL" && info.indexedDocuments === 0)
      notice = "Le guide de l'institut n'a pas encore été publié par l'administration : l'assistant ne peut pas répondre avec des informations officielles.";
    else if (info.kind !== "GENERAL" && info.documents === 0)
      notice = "Aucun document n'a encore été déposé par le professeur : l'assistant répondra avec des connaissances générales.";
    else if (info.kind !== "GENERAL" && info.indexedDocuments < info.documents)
      notice = `${info.indexedDocuments} document(s) sur ${info.documents} sont prêts pour l'assistant (les autres sont en cours d'analyse ou non lisibles).`;
  }
  const suggestions = scope ? COURSE_SUGGESTIONS : GENERAL_SUGGESTIONS;

  const sessionList = (
    <ul className="mt-3 space-y-1">
      {loading && <li className="px-3 py-2 text-sm text-slate-500">Chargement...</li>}
      {!loading && sessions.length === 0 && <li className="px-3 py-2 text-sm text-slate-500">Aucune conversation.</li>}
      {sessions.map((s) => (
        <li key={s.id} className="flex items-center gap-1">
          <button
            onClick={() => openSession(s.id)}
            className={`min-w-0 flex-1 rounded-xl px-3 py-2 text-left text-sm ${
              s.id === activeId ? "bg-blue-50 text-blue-700" : "text-slate-600 hover:bg-slate-50"
            }`}
          >
            <span className="block truncate font-medium">{s.title}</span>
            <span className="block text-xs text-slate-400">
              {new Date(s.updatedAt).toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
            </span>
          </button>
          <button
            onClick={() => remove(s.id)}
            aria-label="Supprimer la conversation"
            className="rounded-lg px-2 py-1 text-slate-400 hover:bg-red-50 hover:text-red-600"
          >
            ✕
          </button>
        </li>
      ))}
    </ul>
  );

  const conversation = (
    <section
      className={`flex min-h-0 flex-col bg-white ${
        compact ? "h-full" : "min-h-[28rem] rounded-2xl border border-slate-200 shadow-sm lg:h-[calc(100vh-15rem)]"
      }`}
    >
      {compact && (
        <div className="flex items-center gap-2 border-b border-slate-200 p-3">
          <select
            value={activeId ?? ""}
            onChange={(e) => (e.target.value ? openSession(Number(e.target.value)) : newChat())}
            className="min-w-0 flex-1 rounded-xl border border-slate-200 px-3 py-2 text-sm"
          >
            <option value="">Nouvelle conversation</option>
            {sessions.map((s) => (
              <option key={s.id} value={s.id}>{s.title}</option>
            ))}
          </select>
          <button onClick={newChat} className={smallBtn}>+ Nouvelle</button>
          {activeId && (
            <button onClick={() => remove(activeId)} className={`${smallBtn} text-red-600`}>Supprimer</button>
          )}
        </div>
      )}

      <div className="flex-1 space-y-4 overflow-y-auto p-5">
        {messages.length === 0 && !sending && (
          <div className="mx-auto mt-4 max-w-lg text-center">
            <p className="text-lg font-semibold text-slate-900">Bonjour {me.firstName} 👋</p>
            <p className="mt-1 text-sm text-slate-500">
              {scope
                ? `Je réponds à partir des documents déposés par vos professeurs pour « ${scope.label} ».`
                : "Je réponds à vos questions sur le fonctionnement de l'institut à partir du guide officiel. Pour une question sur un cours, utilisez le bouton « Poser une question » dans Mes cours."}
            </p>
            <div className="mt-5 flex flex-col gap-2">
              {suggestions.map((s) => (
                <button key={s} onClick={() => setInput(s)} className={`${smallBtn} py-2 text-left`}>{s}</button>
              ))}
            </div>
          </div>
        )}
        {messages.map((m) => (
          <div key={m.id} className={m.role === "USER" ? "flex justify-end" : "flex justify-start"}>
            <div
              className={`max-w-[88%] rounded-2xl px-4 py-3 text-[15px] ${
                m.role === "USER" ? "bg-blue-600 text-white" : "border border-slate-200 bg-white text-slate-800 shadow-sm"
              }`}
            >
              {m.role === "USER" ? <p className="whitespace-pre-wrap">{m.content}</p> : <RichText text={m.content} />}
              {m.sources && m.sources.length > 0 && (
                <div className="mt-3 border-t border-slate-100 pt-2">
                  <p className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">Sources</p>
                  <div className="flex flex-wrap gap-2">
                    {m.sources.map((s) => (
                      <button
                        key={s.n}
                        onClick={() => openSource(s)}
                        title={s.kind === "INSTITUTE" ? "Guide de l'institut" : `${s.courseTitle} · ${s.subjectName}`}
                        className="rounded-lg bg-slate-50 px-2.5 py-1 text-xs text-slate-700 ring-1 ring-slate-200 hover:bg-blue-50 hover:text-blue-700"
                      >
                        [{s.n}] {s.documentTitle}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        ))}
        {sending && (
          <div className="flex justify-start">
            <div className="rounded-2xl border border-slate-200 px-4 py-3 text-sm text-slate-500">L&apos;assistant réfléchit...</div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="border-t border-slate-200 p-4">
        {error && <p role="alert" className="mb-3 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700">{error}</p>}
        {notice && <p className="mb-3 rounded-xl bg-orange-50 px-4 py-2 text-xs text-orange-700">{notice}</p>}
        <div className="flex gap-3">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            rows={2}
            maxLength={2000}
            disabled={sending}
            placeholder="Posez votre question… (Entrée pour envoyer, Maj+Entrée pour un retour à la ligne)"
            className="min-w-0 flex-1 resize-none rounded-xl border border-slate-200 px-4 py-2.5 text-[15px] focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100 disabled:bg-slate-50"
          />
          <button onClick={send} disabled={sending || !input.trim()} className={`${primaryBtn} self-end`}>
            {sending ? "..." : "Envoyer"}
          </button>
        </div>
        <p className="mt-2 text-[11px] text-slate-400">
          Les réponses sont générées par une IA (service Groq) et peuvent contenir des erreurs : vérifiez les informations importantes dans les sources.
        </p>
      </div>
    </section>
  );

  if (compact) return conversation;

  return (
    <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
      <aside className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm lg:h-[calc(100vh-15rem)] lg:overflow-y-auto">
        <button onClick={newChat} className={`${primaryBtn} w-full`}>+ Nouvelle conversation</button>
        {sessionList}
      </aside>
      {conversation}
    </div>
  );
}