"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";
import type { Timetable } from "@/lib/types";

type Row = {
  id: number;
  day: string;
  start: string;
  end: string;
  room: string;
  rawClass: string;
  rawSubject: string;
  rawProfessor: string;
  classId: number | null;
  subjectId: number | null;
  professorId: number | null;
  issues: string[];
  conflict: string | null;
  include: boolean;
};
type Edit = Row & { error?: string | null; done?: boolean };
type Job = { id: string; status: "PROCESSING" | "DONE" | "FAILED"; message: string | null; totalPages: number; donePages: number; warnings: string[]; rows: Row[] };
type ClassItem = { id: number; name: string; subjects: { id: number; name: string; code: string }[] };
type SubjectItem = { id: number; name: string; code: string; professors: { id: number; firstName: string; lastName: string }[] };
type Person = { id: number; firstName: string; lastName: string };
type ConfirmResponse = { timetableId: number; created: number; failed: number; results: { index: number; created: boolean; error: string | null }[] };

const DAYS = [["MONDAY", "Lundi"], ["TUESDAY", "Mardi"], ["WEDNESDAY", "Mercredi"], ["THURSDAY", "Jeudi"], ["FRIDAY", "Vendredi"], ["SATURDAY", "Samedi"], ["SUNDAY", "Dimanche"]];

const valid = (r: Edit) => !!(r.classId && r.subjectId && r.professorId && r.day && r.start && r.end && r.start < r.end);
const cell = "w-full min-w-28 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-sm";

export default function TimetableImportPage() {
  const [timetables, setTimetables] = useState<Timetable[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [professors, setProfessors] = useState<Person[]>([]);
  const [mode, setMode] = useState<"existing" | "new">("existing");
  const [ttId, setTtId] = useState("");
  const [year, setYear] = useState("");
  const [semester, setSemester] = useState("");
  const [classId, setClassId] = useState("");
  const [job, setJob] = useState<Job | null>(null);
  const [rows, setRows] = useState<Edit[]>([]);
  const [createdTt, setCreatedTt] = useState<number | null>(null);
  const [created, setCreated] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    apiFetch<Timetable[]>("/api/admin/timetables").then((t) => { setTimetables(t); if (t[0]) setTtId(String(t[0].id)); else setMode("new"); }).catch(() => {});
    apiFetch<ClassItem[]>("/api/admin/classes").then(setClasses).catch(() => {});
    apiFetch<SubjectItem[]>("/api/admin/subjects").then(setSubjects).catch(() => {});
    apiFetch<Person[]>("/api/admin/professors").then(setProfessors).catch(() => {});
  }, []);

  // follow the background analysis
  useEffect(() => {
    if (!job || job.status !== "PROCESSING") return;
    const timer = setInterval(async () => {
      try {
        const j = await apiFetch<Job>(`/api/admin/timetable-imports/${job.id}`);
        setJob(j);
        if (j.status === "DONE") setRows(j.rows.map((r) => ({ ...r })));
      } catch (e) {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      }
    }, 3000);
    return () => clearInterval(timer);
  }, [job]);

  async function start(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      if (mode === "existing" && ttId) fd.append("timetableId", ttId);
      if (classId) fd.append("classId", classId);
      setJob(await apiFetch<Job>("/api/admin/timetable-imports", { method: "POST", body: fd }));
      setRows([]);
      setCreated(0);
      setCreatedTt(null);
    } catch (err) {
      if (err instanceof ApiError)
        setError(err.status === 503 ? "L'assistant IA n'est pas disponible (clé Groq absente ou service occupé)." : err.status === 400 ? "PDF refusé : il doit contenir du texte (les documents scannés ne sont pas pris en charge)." : err.message);
    } finally {
      setBusy(false);
    }
  }

  const patch = (id: number, p: Partial<Edit>) => setRows((prev) => prev.map((r) => (r.id === id ? { ...r, ...p, error: null } : r)));

  function subjectOptions(r: Edit) {
    const c = classes.find((x) => x.id === r.classId);
    return c && c.subjects.length > 0 ? c.subjects : subjects;
  }
  function professorOptions(r: Edit): Person[] {
    const s = subjects.find((x) => x.id === r.subjectId);
    return s && s.professors.length > 0 ? s.professors : professors;
  }
  function chooseSubject(r: Edit, id: number | null) {
    const s = subjects.find((x) => x.id === id);
    const keep = s?.professors.some((p) => p.id === r.professorId) || (s?.professors.length === 0 && r.professorId);
    patch(r.id, { subjectId: id, professorId: keep ? r.professorId : s?.professors.length === 1 ? s.professors[0].id : null });
  }

  async function confirm() {
    if (!job) return;
    const todo = rows.filter((r) => r.include && !r.done && valid(r));
    if (todo.length === 0) return;
    if (mode === "new" && !createdTt && (!year.trim() || !semester.trim())) {
      setError("Indiquez l'année universitaire et le semestre du nouvel emploi du temps.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await apiFetch<ConfirmResponse>(`/api/admin/timetable-imports/${job.id}/confirm`, {
        method: "POST",
        body: JSON.stringify({
          timetableId: createdTt ?? (mode === "existing" ? Number(ttId) : null),
          academicYear: year.trim() || null,
          semester: semester.trim() || null,
          rows: todo.map((r) => ({ classId: r.classId, subjectId: r.subjectId, professorId: r.professorId, day: r.day, start: r.start, end: r.end, room: r.room })),
        }),
      });
      setCreatedTt(res.timetableId);
      setCreated((c) => c + res.created);
      const outcome = new Map(res.results.map((x) => [todo[x.index].id, x]));
      setRows((prev) => prev.map((r) => {
        const o = outcome.get(r.id);
        return !o ? r : o.created ? { ...r, done: true, include: false, error: null } : { ...r, error: o.error };
      }));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const processing = job?.status === "PROCESSING";
  const pending = rows.filter((r) => r.include && !r.done && valid(r)).length;

  return (
    <AdminShell>
      <PageHeader
        title="Importer un emploi du temps"
        subtitle="Déposez le PDF de l'emploi du temps : l'IA propose les séances, vous vérifiez et corrigez, puis vous les créez. Rien n'est créé avant votre confirmation."
        action={<Link href="/admin/timetables" className={smallBtn}>Emplois du temps</Link>}
      />
      <ErrorBox message={error} />

      {!job && (
        <form onSubmit={start} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2 flex flex-wrap gap-6 text-sm text-slate-700">
              <label className="flex items-center gap-2"><input type="radio" checked={mode === "existing"} disabled={timetables.length === 0} onChange={() => setMode("existing")} /> Ajouter à un emploi du temps existant</label>
              <label className="flex items-center gap-2"><input type="radio" checked={mode === "new"} onChange={() => setMode("new")} /> Créer un nouvel emploi du temps</label>
            </div>
            {mode === "existing" ? (
              <label className="block text-sm font-medium text-slate-700">
                Emploi du temps
                <select value={ttId} onChange={(e) => setTtId(e.target.value)} className={inputCls}>
                  {timetables.map((t) => <option key={t.id} value={t.id}>{t.academicYear} - {t.semester}</option>)}
                </select>
              </label>
            ) : (
              <>
                <TextField label="Année universitaire (ex. 2026-2027)" value={year} onChange={(e) => setYear(e.target.value)} />
                <TextField label="Semestre (ex. S1)" value={semester} onChange={(e) => setSemester(e.target.value)} />
              </>
            )}
            <label className="block text-sm font-medium text-slate-700">
              Classe du document (si le PDF ne concerne qu'une classe)
              <select value={classId} onChange={(e) => setClassId(e.target.value)} className={inputCls}>
                <option value="">Détecter dans le document</option>
                {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Fichier PDF
              <input ref={fileRef} type="file" accept=".pdf" required className="mt-1.5 block w-full text-sm text-slate-700 file:mr-3 file:rounded-lg file:border file:border-slate-200 file:bg-white file:px-3 file:py-2" />
            </label>
          </div>
          <button type="submit" disabled={busy} className={`mt-6 ${primaryBtn}`}>{busy ? "Envoi..." : "Analyser le PDF"}</button>
          <p className="mt-3 text-xs text-slate-500">Le texte du PDF est envoyé au service d&apos;IA pour être structuré. Les PDF scannés (images) ne sont pas pris en charge.</p>
        </form>
      )}

      {job && processing && (
        <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">Analyse en cours...</h2>
          <p className="mt-1 text-sm text-slate-500">Page {job.donePages} sur {job.totalPages}. L&apos;analyse peut durer une minute ou deux pour un document de plusieurs pages.</p>
          <div className="mt-4 h-2 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-blue-500 transition-all" style={{ width: `${(job.donePages / Math.max(1, job.totalPages)) * 100}%` }} /></div>
        </section>
      )}

      {job?.status === "FAILED" && (
        <p className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          L&apos;analyse a échoué : {job.message ?? "erreur inconnue"}.{" "}
          <button onClick={() => { setJob(null); setRows([]); }} className="font-semibold underline">Réessayer</button>
        </p>
      )}

      {job?.status === "DONE" && (
        <>
          {job.warnings.map((w) => <p key={w} className="mt-4 rounded-xl bg-orange-50 px-4 py-3 text-sm text-orange-700">{w}</p>)}
          {job.message && <p className="mt-4 rounded-xl bg-orange-50 px-4 py-3 text-sm text-orange-700">{job.message}</p>}
          <p className="mt-4 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Vérifiez chaque ligne : l&apos;IA peut se tromper. Les lignes en rouge ont un problème à corriger ; seules les lignes cochées et complètes seront créées.
          </p>

          <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-3 py-3"></th><th className="px-3 py-3">Jour</th><th className="px-3 py-3">Début</th><th className="px-3 py-3">Fin</th>
                  <th className="px-3 py-3">Salle</th><th className="px-3 py-3">Classe</th><th className="px-3 py-3">Matière</th><th className="px-3 py-3">Professeur</th><th className="px-3 py-3">Remarques</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.length === 0 && <tr><td colSpan={9} className="px-5 py-8 text-center text-slate-500">Aucune séance trouvée.</td></tr>}
                {rows.map((r) => {
                  const bad = !valid(r) || r.error || (r.conflict && !r.done);
                  return (
                    <tr key={r.id} className={r.done ? "bg-emerald-50" : bad ? "bg-red-50/60" : ""}>
                      <td className="px-3 py-2"><input type="checkbox" checked={r.include && !r.done} disabled={r.done} onChange={(e) => patch(r.id, { include: e.target.checked })} className="h-4 w-4 accent-blue-600" /></td>
                      <td className="px-3 py-2"><select value={r.day} onChange={(e) => patch(r.id, { day: e.target.value })} className={cell} disabled={r.done}><option value="">?</option>{DAYS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select></td>
                      <td className="px-3 py-2"><input type="time" value={r.start} onChange={(e) => patch(r.id, { start: e.target.value })} className={cell} disabled={r.done} /></td>
                      <td className="px-3 py-2"><input type="time" value={r.end} onChange={(e) => patch(r.id, { end: e.target.value })} className={cell} disabled={r.done} /></td>
                      <td className="px-3 py-2"><input value={r.room} onChange={(e) => patch(r.id, { room: e.target.value })} className={cell} disabled={r.done} /></td>
                      <td className="px-3 py-2"><select value={r.classId ?? ""} onChange={(e) => patch(r.id, { classId: e.target.value ? Number(e.target.value) : null })} className={cell} disabled={r.done}><option value="">?</option>{classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>{!r.classId && r.rawClass && <span className="block text-[11px] text-slate-400">{r.rawClass}</span>}</td>
                      <td className="px-3 py-2"><select value={r.subjectId ?? ""} onChange={(e) => chooseSubject(r, e.target.value ? Number(e.target.value) : null)} className={cell} disabled={r.done}><option value="">?</option>{subjectOptions(r).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select>{!r.subjectId && r.rawSubject && <span className="block text-[11px] text-slate-400">{r.rawSubject}</span>}</td>
                      <td className="px-3 py-2"><select value={r.professorId ?? ""} onChange={(e) => patch(r.id, { professorId: e.target.value ? Number(e.target.value) : null })} className={cell} disabled={r.done}><option value="">?</option>{professorOptions(r).map((p) => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}</select>{!r.professorId && r.rawProfessor && <span className="block text-[11px] text-slate-400">{r.rawProfessor}</span>}</td>
                      <td className="max-w-56 px-3 py-2 text-xs">
                        {r.done && <span className="font-semibold text-emerald-700">Créée</span>}
                        {r.error && <span className="text-red-700">{r.error}</span>}
                        {!r.done && !r.error && r.conflict && <span className="text-red-700">{r.conflict}</span>}
                        {!r.done && !r.error && !r.conflict && r.issues.map((i) => <span key={i} className="block text-red-700">{i}</span>)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-4">
            <button onClick={confirm} disabled={busy || pending === 0} className={primaryBtn}>{busy ? "Création..." : `Créer les séances (${pending})`}</button>
            <button onClick={() => { setJob(null); setRows([]); }} className={smallBtn}>Recommencer</button>
            {created > 0 && <span className="text-sm font-medium text-emerald-700">{created} séance(s) créée(s). <Link href="/admin/timetables" className="underline">Voir l&apos;emploi du temps</Link></span>}
          </div>
        </>
      )}
    </AdminShell>
  );
}