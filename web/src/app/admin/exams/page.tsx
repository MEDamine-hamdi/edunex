"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Tabs } from "@/components/Tabs";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ACTION_LABEL, EventRow, ExamItem, fullDate, hour, stamp } from "@/lib/exams";
import { ApiError, apiFetch } from "@/lib/api";

type Lookups = { subjects: { id: number; name: string; code: string; classIds: number[] }[]; classes: { id: number; name: string }[] };
type ProfRow = { id: number; name: string; email: string; kind: string; upcoming: number; past: number };

const TYPES = ["Examen", "Devoir surveillé", "Rattrapage", "Autre"];
const empty = { id: null as number | null, subjectId: "", classIds: [] as number[], examType: "Examen", date: "", startTime: "08:30", endTime: "10:30", room: "", needed: "2" };

export default function AdminExamsPage() {
  const [tab, setTab] = useState("exams");
  const [exams, setExams] = useState<ExamItem[]>([]);
  const [lookups, setLookups] = useState<Lookups>({ subjects: [], classes: [] });
  const [profs, setProfs] = useState<ProfRow[]>([]);
  const [history, setHistory] = useState<EventRow[]>([]);
  const [histProf, setHistProf] = useState("");
  const [form, setForm] = useState(empty);
  const [showForm, setShowForm] = useState(false);
  const [pick, setPick] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const fail = (e: unknown) => {
    if (!(e instanceof ApiError)) return;
    setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les examens." : e.status === 409 ? `Conflit : ${e.message}` : e.message);
  };

  const load = useCallback(async () => {
    try {
      const [x, l, p] = await Promise.all([
        apiFetch<ExamItem[]>("/api/admin/exams"),
        apiFetch<Lookups>("/api/admin/exams/lookups"),
        apiFetch<ProfRow[]>("/api/admin/exams/supervisors"),
      ]);
      setExams(x);
      setLookups(l);
      setProfs(p);
      setError(null);
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) fail(e);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    try {
      setHistory(await apiFetch<EventRow[]>(`/api/admin/exams/history${histProf ? `?professorId=${histProf}` : ""}`));
    } catch (e) {
      fail(e);
    }
  }, [histProf]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (tab === "history") loadHistory();
  }, [tab, loadHistory]);

  async function save(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    try {
      await apiFetch(form.id ? `/api/admin/exams/${form.id}` : "/api/admin/exams", {
        method: form.id ? "PUT" : "POST",
        body: JSON.stringify({ subjectId: Number(form.subjectId), classIds: form.classIds, examType: form.examType, date: form.date, startTime: form.startTime, endTime: form.endTime, room: form.room.trim() || null, supervisorsNeeded: Number(form.needed) }),
      });
      setForm(empty);
      setShowForm(false);
      setInfo("Examen enregistré : les étudiants (et les vacataires, pour un nouvel examen) sont prévenus.");
      await load();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) setError(`Examen refusé : ${err.message}`);
      else fail(err);
    }
  }

  function edit(x: ExamItem) {
    setForm({ id: x.id, subjectId: String(x.subjectId), classIds: x.classes.map((c) => c.id), examType: x.examType, date: x.date, startTime: hour(x.startTime), endTime: hour(x.endTime), room: x.room ?? "", needed: String(x.needed) });
    setShowForm(true);
  }

  async function cancel(x: ExamItem) {
    const reason = window.prompt(`Annuler l'examen « ${x.subjectName} » ? Motif (facultatif)`);
    if (reason === null) return;
    try {
      await apiFetch(`/api/admin/exams/${x.id}/cancel`, { method: "POST", body: JSON.stringify({ reason }) });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  async function assign(x: ExamItem) {
    const id = pick[x.id];
    if (!id) return;
    setError(null);
    setInfo(null);
    try {
      const r = await apiFetch<{ warning: string | null }>(`/api/admin/exams/${x.id}/supervisors`, { method: "POST", body: JSON.stringify({ professorId: Number(id) }) });
      setInfo(r.warning ? `Affecté. Attention : ${r.warning}.` : "Surveillant affecté et prévenu.");
      setPick({ ...pick, [x.id]: "" });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  async function remove(x: ExamItem, professorId: number) {
    const reason = window.prompt("Motif du retrait (facultatif)");
    if (reason === null) return;
    try {
      await apiFetch(`/api/admin/exams/${x.id}/supervisors/${professorId}?reason=${encodeURIComponent(reason)}`, { method: "DELETE" });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  async function setKind(p: ProfRow, kind: string) {
    try {
      await apiFetch(`/api/admin/exams/supervisors/${p.id}/kind`, { method: "PUT", body: JSON.stringify({ kind }) });
      setProfs((prev) => prev.map((x) => (x.id === p.id ? { ...x, kind } : x)));
    } catch (err) {
      fail(err);
    }
  }

  const subject = lookups.subjects.find((s) => String(s.id) === form.subjectId);
  const today = new Date().toISOString().slice(0, 10);

  return (
    <AdminShell>
      <PageHeader title="Examens et surveillance" subtitle="Planifiez les examens, affectez les professeurs permanents et suivez les choix des vacataires." />
      <Tabs tabs={[{ key: "exams", label: "Examens" }, { key: "profs", label: "Surveillants" }, { key: "history", label: "Historique" }]} active={tab} onChange={setTab} />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {tab === "exams" && (
        <>
          <div className="mt-6"><button onClick={() => { setForm(empty); setShowForm((v) => !v); }} className={primaryBtn}>{showForm ? "Fermer" : "+ Nouvel examen"}</button></div>
          {showForm && (
            <form onSubmit={save} className="mt-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="grid gap-4 sm:grid-cols-3">
                <label className="block text-sm font-medium text-slate-700">Matière
                  <select required value={form.subjectId} onChange={(e) => setForm({ ...form, subjectId: e.target.value, classIds: [] })} className={inputCls}><option value="">Choisir...</option>{lookups.subjects.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}</select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Type
                  <select value={form.examType} onChange={(e) => setForm({ ...form, examType: e.target.value })} className={inputCls}>{TYPES.map((t) => <option key={t} value={t}>{t}</option>)}</select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Surveillants nécessaires
                  <input type="number" min={1} max={20} required value={form.needed} onChange={(e) => setForm({ ...form, needed: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">Date<input type="date" required min={form.id ? undefined : today} value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700">Début<input type="time" required value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700">Fin<input type="time" required value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} className={inputCls} /></label>
                <label className="block text-sm font-medium text-slate-700 sm:col-span-3">Salle<input value={form.room} maxLength={100} onChange={(e) => setForm({ ...form, room: e.target.value })} className={inputCls} /></label>
                <fieldset className="sm:col-span-3">
                  <legend className="text-sm font-medium text-slate-700">Classes concernées</legend>
                  <div className="mt-2 flex flex-wrap gap-3">
                    {!subject && <p className="text-sm text-slate-500">Choisissez d&apos;abord une matière.</p>}
                    {subject && subject.classIds.length === 0 && <p className="text-sm text-slate-500">Aucune classe ne suit cette matière.</p>}
                    {subject?.classIds.map((cid) => (
                      <label key={cid} className="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700">
                        <input type="checkbox" className="h-4 w-4 accent-blue-600" checked={form.classIds.includes(cid)} onChange={() => setForm({ ...form, classIds: form.classIds.includes(cid) ? form.classIds.filter((x) => x !== cid) : [...form.classIds, cid] })} />
                        {lookups.classes.find((c) => c.id === cid)?.name}
                      </label>
                    ))}
                  </div>
                </fieldset>
              </div>
              <button type="submit" disabled={form.classIds.length === 0} className={`mt-6 ${primaryBtn}`}>Enregistrer</button>
            </form>
          )}

          <div className="mt-6 space-y-4">
            {exams.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucun examen planifié.</p>}
            {exams.map((x) => {
              const cancelled = x.status === "CANCELLED";
              const missing = x.needed - x.supervisors.length;
              const over = x.date < today;
              return (
                <article key={x.id} className={`rounded-2xl border bg-white p-5 shadow-sm ${cancelled ? "border-red-200 opacity-70" : "border-slate-200"}`}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="text-lg font-semibold text-slate-900">{x.subjectName} <span className="text-sm font-normal text-slate-500">({x.examType})</span></h2>
                      <p className="text-sm text-slate-600">{fullDate(x.date)} · {hour(x.startTime)}-{hour(x.endTime)} · Salle {x.room ?? "-"}</p>
                      <p className="text-xs text-slate-500">{x.classes.map((c) => c.name).join(", ")} · {x.students} étudiants</p>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      {cancelled ? <span className="rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700">Annulé</span>
                        : <span className={`rounded-full px-3 py-1 text-xs font-semibold ${missing > 0 ? "bg-orange-100 text-orange-700" : "bg-emerald-100 text-emerald-700"}`}>{x.supervisors.length}/{x.needed} surveillants{missing > 0 ? ` : il en manque ${missing}` : ""}</span>}
                      {!cancelled && !over && <button onClick={() => edit(x)} className={smallBtn}>Modifier</button>}
                      {!cancelled && !over && <button onClick={() => cancel(x)} className={`${smallBtn} text-red-600`}>Annuler</button>}
                    </div>
                  </div>
                  {!cancelled && (
                    <div className="mt-4 border-t border-slate-100 pt-4">
                      <div className="flex flex-wrap gap-2">
                        {x.supervisors.length === 0 && <span className="text-sm text-slate-400">Aucun surveillant.</span>}
                        {x.supervisors.map((s) => (
                          <span key={s.professorId} className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-3 py-1 text-sm text-slate-700">
                            {s.name}
                            <span className={`rounded-full px-2 text-[11px] font-semibold ${s.kind === "VACATAIRE" ? "bg-amber-100 text-amber-800" : "bg-emerald-100 text-emerald-800"}`}>{s.kind === "VACATAIRE" ? "Vacataire" : "Permanent"}</span>
                            <span className="text-[11px] text-slate-400">{s.mode === "VOLUNTEER" ? "volontaire" : "affecté"}</span>
                            {!over && <button onClick={() => remove(x, s.professorId)} aria-label="Retirer" className="text-slate-400 hover:text-red-600">✕</button>}
                          </span>
                        ))}
                      </div>
                      {!over && missing > 0 && (
                        <div className="mt-3 flex flex-wrap items-center gap-2">
                          <select value={pick[x.id] ?? ""} onChange={(e) => setPick({ ...pick, [x.id]: e.target.value })} className={`${inputCls} mt-0 w-72`}>
                            <option value="">Affecter un professeur...</option>
                            {profs.filter((p) => !x.supervisors.some((s) => s.professorId === p.id)).map((p) => <option key={p.id} value={p.id}>{p.name} ({p.kind === "VACATAIRE" ? "vacataire" : "permanent"})</option>)}
                          </select>
                          <button onClick={() => assign(x)} disabled={!pick[x.id]} className={primaryBtn}>Affecter</button>
                        </div>
                      )}
                    </div>
                  )}
                  {cancelled && x.cancelReason && <p className="mt-3 text-sm text-red-700">Motif : {x.cancelReason}</p>}
                </article>
              );
            })}
          </div>
        </>
      )}

      {tab === "profs" && (
        <>
          <p className="mt-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600">Un professeur <b>vacataire</b> choisit lui-même les examens qu&apos;il surveille. Un professeur <b>permanent</b> est affecté par l&apos;administration et ne peut pas refuser. Un professeur sans choix enregistré est considéré comme permanent.</p>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}><tr><th className="px-5 py-3">Professeur</th><th className="px-5 py-3">Statut</th><th className="px-5 py-3">Surveillances à venir</th><th className="px-5 py-3">Passées</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {profs.map((p) => (
                  <tr key={p.id}>
                    <td className="px-5 py-3 font-medium text-slate-900">{p.name}<span className="block text-xs font-normal text-slate-500">{p.email}</span></td>
                    <td className="px-5 py-3">
                      <select value={p.kind} onChange={(e) => setKind(p, e.target.value)} className={`rounded-lg border px-2 py-1 text-xs font-semibold ${p.kind === "VACATAIRE" ? "border-amber-300 bg-amber-50 text-amber-800" : "border-emerald-300 bg-emerald-50 text-emerald-800"}`}>
                        <option value="PERMANENT">Permanent</option>
                        <option value="VACATAIRE">Vacataire</option>
                      </select>
                    </td>
                    <td className="px-5 py-3 text-slate-700">{p.upcoming}</td>
                    <td className="px-5 py-3 text-slate-700">{p.past}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === "history" && (
        <>
          <div className="mt-6"><select value={histProf} onChange={(e) => setHistProf(e.target.value)} className={`${inputCls} mt-0 w-72`}><option value="">Tous les professeurs</option>{profs.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></div>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}><tr><th className="px-5 py-3">Date</th><th className="px-5 py-3">Professeur</th><th className="px-5 py-3">Action</th><th className="px-5 py-3">Examen</th><th className="px-5 py-3">Par</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {history.length === 0 && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucun historique.</td></tr>}
                {history.map((h) => (
                  <tr key={h.id}>
                    <td className="px-5 py-3 text-slate-600">{stamp(h.at)}</td>
                    <td className="px-5 py-3 font-medium text-slate-900">{h.professor}</td>
                    <td className="px-5 py-3 text-slate-700">{ACTION_LABEL[h.action] ?? h.action}{h.reason && <span className="block text-xs text-slate-500">{h.reason}</span>}</td>
                    <td className="px-5 py-3 text-slate-700">{h.exam ?? "-"}</td>
                    <td className="px-5 py-3 text-slate-600">{h.actor ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </AdminShell>
  );
}