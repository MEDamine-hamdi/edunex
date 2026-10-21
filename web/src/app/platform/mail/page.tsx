"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { PlatformShell } from "@/components/PlatformShell";
import { Panel, StatCard } from "@/components/Stats";
import { ErrorBox, PageHeader, TextField, primaryBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { formatDate } from "@/lib/format";

type Status = {
  configured: boolean;
  senderEmail: string;
  pending: number;
  sent: number;
  failed: number;
  recentFailures: { to: string; subject: string; error: string; createdAt: string }[];
};

export default function MailPage() {
  const [status, setStatus] = useState<Status | null>(null);
  const [to, setTo] = useState("");
  const [busy, setBusy] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setStatus(await apiFetch<Status>("/api/platform/mail/status"));
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, []);

  useEffect(() => {
    load();
    const t = setInterval(load, 15000);
    return () => clearInterval(t);
  }, [load]);

  async function sendTest(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      await apiFetch("/api/platform/mail/test", { method: "POST", body: JSON.stringify({ to: to.trim() }) });
      setInfo("E-mail de test mis en file d'attente : il part dans quelques secondes.");
      setTimeout(load, 4000);
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 400 ? "L'envoi d'e-mails n'est pas configuré (clé Brevo et expéditeur)." : err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <PlatformShell>
      <PageHeader title="E-mails" subtitle="Envoi des notifications par e-mail via Brevo." />
      <ErrorBox message={error} />
      {status && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Configuration" value={status.configured ? "Active" : "Non configurée"} tone={status.configured ? "green" : "orange"} hint={status.senderEmail || "Ajoutez la clé Brevo et l'expéditeur"} />
            <StatCard label="En attente" value={status.pending} />
            <StatCard label="Envoyés" value={status.sent} tone="green" />
            <StatCard label="Échecs" value={status.failed} tone={status.failed > 0 ? "red" : "slate"} />
          </div>
          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <Panel title="E-mail de test">
              <form onSubmit={sendTest} className="space-y-4">
                <TextField label="Adresse du destinataire" type="email" required value={to} onChange={(e) => setTo(e.target.value)} />
                <button type="submit" disabled={busy || !status.configured} className={primaryBtn}>Envoyer</button>
                {info && <p className="text-sm text-emerald-700">{info}</p>}
              </form>
              <p className="mt-4 text-xs text-slate-500">Plan gratuit Brevo : 300 e-mails par jour. L&apos;expéditeur doit être vérifié dans Brevo.</p>
            </Panel>
            <Panel title="Derniers échecs">
              {status.recentFailures.length === 0 ? (
                <p className="text-sm text-slate-500">Aucun échec.</p>
              ) : (
                <ul className="space-y-3 text-sm">
                  {status.recentFailures.map((f, i) => (
                    <li key={i} className="rounded-xl bg-red-50 px-4 py-3">
                      <p className="font-medium text-slate-900">{f.to} · {formatDate(f.createdAt)}</p>
                      <p className="text-xs text-red-700">{f.error}</p>
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          </div>
        </>
      )}
    </PlatformShell>
  );
}