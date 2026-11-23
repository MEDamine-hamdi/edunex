"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Tabs } from "@/components/Tabs";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { CertRow, Coverage, Detail, RETAKE, RetakeItem, STATUS, explain, hour, period, shortDate, stamp } from "@/lib/certificates";

type Hit = { id: number; name: string; number: string };

function Dialog({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const [d, setD] = useState<Detail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [image, setImage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    apiFetch<Detail>(`/api/admin/certificates/${id}`).then(setD).catch((e) => e instanceof ApiError && setError(explain(e.message, e.status)));
  }, [id]);
  useEffect(() => load(), [load]);

  useEffect(() => {
    if (!d?.row.hasFile || !/\.(png|jpe?g)$/i.test(d.row.fileName ?? "")) {
      setImage(null);
      return;
    }
    let url: string | null = null;
    let off = false;
    fetchBlobUrl(`/api/admin/certificates/${id}/file`).then((u) => {
      if (off) URL.revokeObjectURL(u);
      else {
        url = u;
        setImage(u);
      }
    }).catch(() => {});
    return () => {
      off = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [d?.row.hasFile, d?.row.fileName, id]);

  async function open() {
    try {
      const url = await fetchBlobUrl(`/api/admin/certificates/${id}/file`);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch {
      setError("Ouverture impossible.");
    }
  }

  async function act(path: string, body: unknown) {
    setBusy(true);
    setError(null);
    try {
      await apiFetch(`/api/admin/certificates/${id}/${path}`, { method: "POST", body: JSON.stringify(body) });
      onChanged();
      load();
    } catch (e) {
      if (e instanceof ApiError) setError(explain(e.message, e.status));
    } finally {
      setBusy(false);
    }
  }

  const c = d?.row;
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
      <div className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Certificat médical</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">{c?.studentName ?? "..."}</h2>
            {c && <p className="text-sm text-slate-500">{c.studentNumber} · {period(c.startDate, c.endDate)}</p>}
          </div>
          <button onClick={onClose} className={smallBtn}>Fermer</button>
        </div>
        <ErrorBox message={error} />
        {d && c && (
          <div className="mt-4 space-y-5">
            <div className="flex flex-wrap items-center gap-3">
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS[c.status].cls}`}>{STATUS[c.status].label}</span>
              <span className="text-xs text-slate-500">{c.source === "IN_PERSON" ? "Remis en personne" : "Envoyé par l'application"} · {stamp(c.createdAt)}</span>
            </div>
            {c.note && <p className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-700">{c.note}</p>}
            {c.decidedBy && <p className="text-sm text-slate-600">Décision de {c.decidedBy}{c.decisionNote ? ` : ${c.decisionNote}` : ""}</p>}

            <section>
              <h3 className="text-sm font-semibold text-slate-900">Absences en séance ({d.absences.length})</h3>
              <ul className="mt-2 space-y-1">
                {d.absences.length === 0 && <li className="text-sm text-slate-500">Aucune.</li>}
                {d.absences.map((a) => <li key={a.attendanceId} className="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{shortDate(a.date)} · {a.subjectName}</li>)}
              </ul>
            </section>
            <section>
              <h3 className="text-sm font-semibold text-slate-900">Examens manqués ({d.exams.length})</h3>
              <ul className="mt-2 space-y-1">
                {d.exams.length === 0 && <li className="text-sm text-slate-500">Aucun.</li>}
                {d.exams.map((x) => <li key={x.examId} className="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{x.subjectName} ({x.examType}) · {shortDate(x.date)} {hour(x.startTime)}</li>)}
              </ul>
            </section>

            {c.hasFile ? (
              <section>
                <h3 className="text-sm font-semibold text-slate-900">Le certificat</h3>
                <button onClick={open} className={`mt-2 ${smallBtn}`}>Ouvrir le fichier</button>
                {image && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={image} alt="Certificat" className="mt-3 max-h-96 w-full rounded-xl border border-slate-200 object-contain" />
                )}
              </section>
            ) : <p className="text-sm text-slate-500">Aucun fichier joint (certificat papier remis à l&apos;administration).</p>}

            <div className="flex flex-wrap gap-3 border-t border-slate-100 pt-4">
              {c.status === "PENDING" && (
                <>
                  <button disabled={busy} onClick={() => act("approve", { note: null })} className={primaryBtn}>Valider le certificat</button>
                  <button disabled={busy} onClick={() => { const reason = window.prompt("Motif du refus (visible par l'étudiant)"); if (reason && reason.trim()) act("reject", { reason: reason.trim() }); }} className={`${smallBtn} text-red-600`}>Refuser</button>
                </>
              )}
              {c.status === "APPROVED" && (
                <button disabled={busy} onClick={() => { const reason = window.prompt("Motif de l'annulation de la validation (facultatif)"); if (reason !== null) act("revoke", { note: reason.trim() || null }); }} className={`${smallBtn} text-red-600`}>Annuler la validation</button>
              )}
            </div>
            {d.retakes.length > 0 && (
              <section>
                <h3 className="text-sm font-semibold text-slate-900">Rattrapages créés</h3>
                <ul className="mt-2 space-y-1">
                  {d.retakes.map((r) => <li key={r.id} className="flex justify-between rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700"><span>{r.subjectName} ({r.examType})</span><span className={`rounded-full px-2 text-xs font-semibold ${RETAKE[r.status].cls}`}>{RETAKE[r.status].label}</span></li>)}
                </ul>
              </section>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function InPerson({ onDone }: { onDone: () => void }) {
  const [q, setQ] = useState("");
  const [hits, setHits] = useState<Hit[]>([]);
  const [student, setStudent] = useState<Hit | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [coverage, setCoverage] = useState<Coverage | null>(null);
  const [exams, setExams] = useState<number[]>([]);
  const [note, setNote] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (student) return;
    const t = setTimeout(() => apiFetch<Hit[]>(`/api/admin/certificates/students?q=${encodeURIComponent(q)}`).then(setHits).catch(() => {}), 250);
    return () => clearTimeout(t);
  }, [q, student]);

  useEffect(() => {
    setCoverage(null);
    setExams([]);
    if (!student || !from || !to || to < from) return;
    apiFetch<Coverage>(`/api/admin/certificates/coverage?studentId=${student.id}&from=${from}&to=${to}`).then(setCoverage).catch((e) => e instanceof ApiError && setError(explain(e.message, e.status)));
  }, [student, from, to]);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!student) return;
    setBusy(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("studentId", String(student.id));
      fd.append("startDate", from);
      fd.append("endDate", to);
      if (note.trim()) fd.append("note", note.trim());
      exams.forEach((id) => fd.append("examIds", String(id)));
      if (file) fd.append("file", file);
      await apiFetch("/api/admin/certificates/in-person", { method: "POST", body: fd });
      onDone();
    } catch (err) {
      if (err instanceof ApiError) setError(explain(err.message, err.status));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Certificat remis en personne</h2>
      <p className="mt-1 text-sm text-slate-500">Vous avez vérifié le papier : il compte tout de suite. Les absences de la période sont supprimées et un rattrapage est créé pour chaque examen coché.</p>
      <ErrorBox message={error} />
      {!student ? (
        <div className="mt-4">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher un étudiant (nom ou numéro)" className={`${inputCls} mt-0`} />
          <ul className="mt-2 max-h-48 space-y-1 overflow-y-auto">
            {hits.map((h) => <li key={h.id}><button type="button" onClick={() => setStudent(h)} className="w-full rounded-xl px-3 py-2 text-start text-sm hover:bg-slate-50">{h.name} <span className="text-slate-500">({h.number})</span></button></li>)}
          </ul>
        </div>
      ) : (
        <p className="mt-4 text-sm font-semibold text-slate-900">{student.name} ({student.number}) <button type="button" onClick={() => { setStudent(null); setCoverage(null); }} className="ms-2 text-xs font-medium text-blue-600">changer</button></p>
      )}
      {student && (
        <>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <label className="block text-sm font-medium text-slate-700">Du<input type="date" required value={from} onChange={(e) => setFrom(e.target.value)} className={inputCls} /></label>
            <label className="block text-sm font-medium text-slate-700">Au<input type="date" required value={to} min={from || undefined} onChange={(e) => setTo(e.target.value)} className={inputCls} /></label>
          </div>
          {coverage && (
            <div className="mt-4 space-y-3 text-sm">
              <p className="font-semibold text-slate-900">Absences en séance concernées : {coverage.absences.length}</p>
              {coverage.exams.map((x) => (
                <label key={x.examId} className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2 text-slate-800">
                  <input type="checkbox" className="h-4 w-4 accent-blue-600" checked={exams.includes(x.examId)} onChange={() => setExams((p) => (p.includes(x.examId) ? p.filter((i) => i !== x.examId) : [...p, x.examId]))} />
                  Examen manqué : {x.subjectName} ({x.examType}) · {shortDate(x.date)}
                </label>
              ))}
            </div>
          )}
          <label className="mt-4 block text-sm font-medium text-slate-700">Note<input maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} className={inputCls} /></label>
          <div className="mt-3"><input type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="block w-full text-sm text-slate-600" /><p className="mt-1 text-xs text-slate-500">Scan facultatif (PDF, PNG, JPG, 5 Mo).</p></div>
          <button type="submit" disabled={busy || !coverage} className={`mt-5 ${primaryBtn}`}>{busy ? "Enregistrement..." : "Enregistrer et valider"}</button>
        </>
      )}
    </form>
  );
}

export default function AdminCertificatesPage() {
  const [tab, setTab] = useState("pending");
  const [rows, setRows] = useState<CertRow[]>([]);
  const [retakes, setRetakes] = useState<RetakeItem[]>([]);
  const [openId, setOpenId] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [picked, setPicked] = useState<number[]>([]);
  const [date, setDate] = useState("");
  const [start, setStart] = useState("09:00");
  const [end, setEnd] = useState("11:00");
  const [room, setRoom] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const fail = (e: unknown) => {
    if (e instanceof ApiError && e.status !== 401) setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les certificats." : explain(e.message, e.status));
  };
  const load = useCallback(async () => {
    try {
      const [c, r] = await Promise.all([apiFetch<CertRow[]>("/api/admin/certificates"), apiFetch<RetakeItem[]>("/api/admin/retakes")]);
      setRows(c);
      setRetakes(r);
      setError(null);
    } catch (e) {
      fail(e);
    }
  }, []);
  useEffect(() => {
    load();
  }, [load]);

  async function schedule(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    try {
      await apiFetch("/api/admin/retakes/schedule", { method: "POST", body: JSON.stringify({ retakeIds: picked, date, startTime: start, endTime: end, room: room.trim() || null }) });
      setInfo("Rattrapage planifié : les étudiants sont prévenus.");
      setPicked([]);
      await load();
    } catch (err) {
      fail(err);
    }
  }
  async function simple(path: string) {
    try {
      await apiFetch(path, { method: "POST" });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  const pending = rows.filter((r) => r.status === "PENDING");
  const toSchedule = retakes.filter((r) => r.status === "TO_SCHEDULE");
  const scheduled = retakes.filter((r) => r.status === "SCHEDULED" || r.status === "DONE");

  const list = (items: CertRow[], empty: string) => (
    <div className="mt-4 space-y-3">
      {items.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">{empty}</p>}
      {items.map((c) => (
        <button key={c.id} onClick={() => setOpenId(c.id)} className="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 text-start shadow-sm transition hover:border-blue-300">
          <div>
            <p className="font-semibold text-slate-900">{c.studentName} <span className="font-normal text-slate-500">({c.studentNumber})</span></p>
            <p className="text-sm text-slate-600">{period(c.startDate, c.endDate)}</p>
            <p className="text-xs text-slate-500">{c.source === "IN_PERSON" ? "Remis en personne" : "Envoyé par l'application"} · {stamp(c.createdAt)}</p>
          </div>
          <span className={`rounded-full px-3 py-1 text-xs font-semibold ${STATUS[c.status].cls}`}>{STATUS[c.status].label}</span>
        </button>
      ))}
    </div>
  );

  return (
    <AdminShell>
      <PageHeader title="Certificats médicaux" subtitle="Vérifiez les certificats envoyés par les étudiants, enregistrez ceux remis en personne et planifiez les rattrapages." action={<button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>{showForm ? "Fermer" : "+ Certificat remis en personne"}</button>} />
      {showForm && <InPerson onDone={() => { setShowForm(false); load(); }} />}
      <Tabs tabs={[{ key: "pending", label: `À vérifier (${pending.length})` }, { key: "history", label: "Historique" }, { key: "retakes", label: `Rattrapages (${toSchedule.length})` }]} active={tab} onChange={setTab} />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {tab === "pending" && list(pending, "Aucun certificat à vérifier.")}
      {tab === "history" && list(rows, "Aucun certificat.")}

      {tab === "retakes" && (
        <div className="mt-4 space-y-6">
          <section>
            <h2 className="text-lg font-semibold">À planifier ({toSchedule.length})</h2>
            <div className="mt-3 space-y-2">
              {toSchedule.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun rattrapage à planifier.</p>}
              {toSchedule.map((r) => (
                <label key={r.id} className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm shadow-sm">
                  <input type="checkbox" className="h-4 w-4 accent-blue-600" checked={picked.includes(r.id)} onChange={() => setPicked((p) => (p.includes(r.id) ? p.filter((i) => i !== r.id) : [...p, r.id]))} />
                  <span className="flex-1"><span className="font-medium text-slate-900">{r.studentName}</span> ({r.studentNumber}) · {r.subjectName} ({r.examType}), examen du {shortDate(r.originalDate)}</span>
                </label>
              ))}
            </div>
            {picked.length > 0 && (
              <form onSubmit={schedule} className="mt-4 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-4">
                <label className="block text-sm font-medium text-slate-700">Date<input type="date" required min={new Date().toISOString().slice(0, 10)} value={date} onChange={(e) => setDate(e.target.value)} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700">Début<input type="time" required value={start} onChange={(e) => setStart(e.target.value)} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700">Fin<input type="time" required value={end} onChange={(e) => setEnd(e.target.value)} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700">Salle<input maxLength={100} value={room} onChange={(e) => setRoom(e.target.value)} className={inputCls} /></label>
                <div className="sm:col-span-4"><button type="submit" className={primaryBtn}>Planifier pour {picked.length} étudiant(s)</button></div>
              </form>
            )}
          </section>
          <section>
            <h2 className="text-lg font-semibold">Planifiés et passés</h2>
            <div className="mt-3 space-y-2">
              {scheduled.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun.</p>}
              {scheduled.map((r) => (
                <div key={r.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm shadow-sm">
                  <span><span className="font-medium text-slate-900">{r.studentName}</span> · {r.subjectName} · {r.date ? `${shortDate(r.date)} ${hour(r.startTime)}-${hour(r.endTime)}${r.room ? ` · salle ${r.room}` : ""}` : ""}</span>
                  <span className="flex items-center gap-2">
                    <span className={`rounded-full px-2 text-xs font-semibold ${RETAKE[r.status].cls}`}>{RETAKE[r.status].label}</span>
                    {r.status === "SCHEDULED" && <button onClick={() => simple(`/api/admin/retakes/${r.id}/done`)} className={smallBtn}>Marquer passé</button>}
                    {r.status === "SCHEDULED" && <button onClick={() => window.confirm("Annuler ce rattrapage ?") && simple(`/api/admin/retakes/${r.id}/cancel`)} className={`${smallBtn} text-red-600`}>Annuler</button>}
                  </span>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
      {openId && <Dialog id={openId} onClose={() => setOpenId(null)} onChanged={load} />}
    </AdminShell>
  );
}