"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { CertRow, Coverage, RETAKE, RetakeItem, STATUS, explain, hour, period, shortDate, stamp } from "@/lib/certificates";

export default function StudentCertificatesPage() {
  const [rows, setRows] = useState<CertRow[]>([]);
  const [retakes, setRetakes] = useState<RetakeItem[]>([]);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [coverage, setCoverage] = useState<Coverage | null>(null);
  const [exams, setExams] = useState<number[]>([]);
  const [note, setNote] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const today = new Date().toISOString().slice(0, 10);

  const load = useCallback(async () => {
    try {
      const [c, r] = await Promise.all([apiFetch<CertRow[]>("/api/student/certificates"), apiFetch<RetakeItem[]>("/api/student/retakes")]);
      setRows(c);
      setRetakes(r);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(explain(e.message, e.status));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // what the certificate would cover
  useEffect(() => {
    setCoverage(null);
    setExams([]);
    if (!from || !to || to < from) return;
    const t = setTimeout(() => {
      apiFetch<Coverage>(`/api/student/certificates/coverage?from=${from}&to=${to}`).then(setCoverage).catch((e) => e instanceof ApiError && setError(explain(e.message, e.status)));
    }, 250);
    return () => clearTimeout(t);
  }, [from, to]);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!file) {
      setError("Joignez le fichier du certificat.");
      return;
    }
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const fd = new FormData();
      fd.append("startDate", from);
      fd.append("endDate", to);
      if (note.trim()) fd.append("note", note.trim());
      exams.forEach((id) => fd.append("examIds", String(id)));
      fd.append("file", file);
      await apiFetch("/api/student/certificates", { method: "POST", body: fd });
      setInfo("Certificat envoyé. Un agent de l'administration va le vérifier ; vous serez prévenu(e) de sa décision.");
      setFrom("");
      setTo("");
      setNote("");
      setFile(null);
      if (fileInput.current) fileInput.current.value = "";
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(explain(err.message, err.status));
    } finally {
      setBusy(false);
    }
  }

  async function open(id: number) {
    try {
      const url = await fetchBlobUrl(`/api/student/certificates/${id}/file`);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch {
      setError("Ouverture impossible.");
    }
  }

  async function withdraw(id: number) {
    if (!window.confirm("Retirer ce certificat ? Il ne sera pas vérifié.")) return;
    try {
      await apiFetch<void>(`/api/student/certificates/${id}`, { method: "DELETE" });
      await load();
    } catch (err) {
      if (err instanceof ApiError) setError(explain(err.message, err.status));
    }
  }

  const nothing = coverage && coverage.absences.length === 0 && coverage.exams.length === 0;

  return (
    <StudentShell>
      <PageHeader title="Certificats médicaux" subtitle="Justifiez une absence en séance ou à un examen. Vous pouvez aussi remettre votre certificat en personne à l'administration." />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Envoyer un certificat par l&apos;application</h2>
        <p className="mt-1 text-sm text-slate-500">Un agent de l&apos;administration vérifie chaque certificat. Une fois validé, vos absences de la période sont supprimées et un rattrapage est proposé pour un examen manqué. À envoyer dans les 7 jours qui suivent la fin de l&apos;absence.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-medium text-slate-700">Du<input type="date" required max={today} value={from} onChange={(e) => setFrom(e.target.value)} className={inputCls} /></label>
          <label className="block text-sm font-medium text-slate-700">Au (dernier jour d&apos;arrêt)<input type="date" required value={to} min={from || undefined} onChange={(e) => setTo(e.target.value)} className={inputCls} /></label>
        </div>

        {coverage && (
          <div className="mt-5 space-y-4">
            <div>
              <p className="text-sm font-semibold text-slate-900">Absences en séance concernées ({coverage.absences.length})</p>
              {coverage.absences.length === 0 && <p className="mt-1 text-sm text-slate-500">Aucune absence enregistrée sur cette période.</p>}
              <ul className="mt-2 space-y-1">
                {coverage.absences.map((a) => <li key={a.attendanceId} className="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{shortDate(a.date)} · {a.subjectName}</li>)}
              </ul>
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Examens manqués : cochez ceux que vous n&apos;avez pas pu passer</p>
              {coverage.exams.length === 0 && <p className="mt-1 text-sm text-slate-500">Aucun examen sur cette période.</p>}
              <div className="mt-2 space-y-2">
                {coverage.exams.map((x) => (
                  <label key={x.examId} className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-800">
                    <input type="checkbox" className="h-4 w-4 accent-blue-600" checked={exams.includes(x.examId)} onChange={() => setExams((p) => (p.includes(x.examId) ? p.filter((i) => i !== x.examId) : [...p, x.examId]))} />
                    {x.subjectName} ({x.examType}) · {shortDate(x.date)} {hour(x.startTime)}
                  </label>
                ))}
              </div>
            </div>
            {nothing && <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">Rien à justifier sur cette période : vérifiez les dates.</p>}
          </div>
        )}

        <label className="mt-5 block text-sm font-medium text-slate-700">Commentaire (facultatif)
          <textarea rows={2} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} className={inputCls} />
        </label>
        <div className="mt-4">
          <input ref={fileInput} type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="block w-full text-sm text-slate-600" />
          <p className="mt-1 text-xs text-slate-500">PDF, PNG ou JPG, 5 Mo maximum : le certificat doit être lisible.</p>
        </div>
        <button type="submit" disabled={busy || !coverage || !!nothing} className={`mt-5 ${primaryBtn}`}>{busy ? "Envoi..." : "Envoyer le certificat"}</button>
      </form>

      <h2 className="mt-8 text-lg font-semibold">Mes certificats</h2>
      <div className="mt-3 space-y-3">
        {rows.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun certificat pour le moment.</p>}
        {rows.map((c) => (
          <article key={c.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="font-semibold text-slate-900">{period(c.startDate, c.endDate)}</p>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS[c.status].cls}`}>{STATUS[c.status].label}</span>
            </div>
            <p className="mt-1 text-xs text-slate-500">{c.source === "IN_PERSON" ? "Remis en personne à l'administration" : "Envoyé par l'application"} · {stamp(c.createdAt)}</p>
            {c.note && <p className="mt-2 text-sm text-slate-700">{c.note}</p>}
            {c.decisionNote && <p className="mt-2 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{c.status === "REJECTED" ? "Motif du refus : " : "Note : "}{c.decisionNote}</p>}
            {c.decidedBy && <p className="mt-1 text-xs text-slate-500">Décision de {c.decidedBy}{c.decidedAt ? ` · ${stamp(c.decidedAt)}` : ""}</p>}
            <div className="mt-3 flex flex-wrap gap-2">
              {c.hasFile && <button onClick={() => open(c.id)} className={smallBtn}>Ouvrir le fichier</button>}
              {c.status === "PENDING" && <button onClick={() => withdraw(c.id)} className={`${smallBtn} text-red-600`}>Retirer</button>}
            </div>
          </article>
        ))}
      </div>

      <h2 className="mt-8 text-lg font-semibold">Mes rattrapages</h2>
      <div className="mt-3 space-y-3">
        {retakes.filter((r) => r.status !== "CANCELLED").length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun rattrapage.</p>}
        {retakes.filter((r) => r.status !== "CANCELLED").map((r) => (
          <article key={r.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div>
              <p className="font-semibold text-slate-900">{r.subjectName} <span className="font-normal text-slate-500">({r.examType})</span></p>
              <p className="text-sm text-slate-600">
                {r.date ? `${shortDate(r.date)} · ${hour(r.startTime)}-${hour(r.endTime)}${r.room ? ` · Salle ${r.room}` : ""}` : "Date à venir : l'administration va planifier votre rattrapage."}
              </p>
              <p className="text-xs text-slate-500">Examen manqué du {shortDate(r.originalDate)}</p>
            </div>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${RETAKE[r.status].cls}`}>{RETAKE[r.status].label}</span>
          </article>
        ))}
      </div>
    </StudentShell>
  );
}