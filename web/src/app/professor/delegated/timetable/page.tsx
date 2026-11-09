"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ProfessorShell } from "@/components/ProfessorShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Prof = { id: number; name: string };
type Subject = { id: number; name: string; code: string; professors: Prof[] };
type ClassItem = { id: number; name: string; subjects: Subject[] };
type Lookups = { timetables: { id: number; label: string }[]; classes: ClassItem[] };

const DAYS: [string, string][] = [["MONDAY", "Lundi"], ["TUESDAY", "Mardi"], ["WEDNESDAY", "Mercredi"], ["THURSDAY", "Jeudi"], ["FRIDAY", "Vendredi"], ["SATURDAY", "Samedi"]];

export default function DelegatedTimetablePage() {
  const [lookups, setLookups] = useState<Lookups>({ timetables: [], classes: [] });
  const [timetableId, setTimetableId] = useState("");
  const [year, setYear] = useState("");
  const [semester, setSemester] = useState("");
  const [day, setDay] = useState("MONDAY");
  const [start, setStart] = useState("08:30");
  const [end, setEnd] = useState("10:00");
  const [room, setRoom] = useState("");
  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [professorId, setProfessorId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    apiFetch<Lookups>("/api/delegated/timetable/lookups").then(setLookups).catch((e) => e instanceof ApiError && e.status !== 401 && setError(e.status === 403 ? "Vous n'avez pas le droit de gérer les emplois du temps." : e.message));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const subjects = lookups.classes.find((c) => String(c.id) === classId)?.subjects ?? [];
  const professors = subjects.find((s) => String(s.id) === subjectId)?.professors ?? [];

  async function createTimetable(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    try {
      await apiFetch("/api/delegated/timetable/timetables", { method: "POST", body: JSON.stringify({ academicYear: year.trim(), semester: semester.trim() }) });
      setYear("");
      setSemester("");
      setInfo("Emploi du temps créé.");
      load();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function addSession(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      await apiFetch("/api/delegated/timetable/sessions", {
        method: "POST",
        body: JSON.stringify({ timetableId: Number(timetableId), dayOfWeek: day, startTime: start, endTime: end, room: room.trim() || null, classId: Number(classId), subjectId: Number(subjectId), professorId: Number(professorId) }),
      });
      setInfo("Séance ajoutée : la classe et le professeur sont prévenus.");
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 409 ? `Conflit d'horaire : ${err.message}` : err.status === 400 ? `Séance refusée : ${err.message}` : err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <ProfessorShell>
      <PageHeader title="Gestion des emplois du temps" subtitle="Droit accordé par l'administration : créez un emploi du temps et ajoutez des séances. Les conflits de salle, de professeur et de classe sont refusés." />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <form onSubmit={createTimetable} className="mt-6 flex flex-wrap items-end gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <label className="block text-sm font-medium text-slate-700">Année universitaire (ex. 2026-2027)<input required value={year} onChange={(e) => setYear(e.target.value)} className={inputCls} /></label>
        <label className="block text-sm font-medium text-slate-700">Semestre (ex. S1)<input required value={semester} onChange={(e) => setSemester(e.target.value)} className={inputCls} /></label>
        <button type="submit" className={primaryBtn}>Créer l&apos;emploi du temps</button>
      </form>

      <form onSubmit={addSession} className="mt-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2 lg:grid-cols-4">
        <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Emploi du temps
          <select required value={timetableId} onChange={(e) => setTimetableId(e.target.value)} className={inputCls}><option value="">Choisir...</option>{lookups.timetables.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Jour
          <select value={day} onChange={(e) => setDay(e.target.value)} className={inputCls}>{DAYS.map(([k, l]) => <option key={k} value={k}>{l}</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Salle<input value={room} onChange={(e) => setRoom(e.target.value)} className={inputCls} /></label>
        <label className="block text-sm font-medium text-slate-700">Début<input type="time" required value={start} onChange={(e) => setStart(e.target.value)} className={inputCls} /></label>
        <label className="block text-sm font-medium text-slate-700">Fin<input type="time" required value={end} onChange={(e) => setEnd(e.target.value)} className={inputCls} /></label>
        <label className="block text-sm font-medium text-slate-700">Classe
          <select required value={classId} onChange={(e) => { setClassId(e.target.value); setSubjectId(""); setProfessorId(""); }} className={inputCls}><option value="">Choisir...</option>{lookups.classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Matière
          <select required value={subjectId} onChange={(e) => { setSubjectId(e.target.value); setProfessorId(""); }} className={inputCls}><option value="">Choisir...</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select>
        </label>
        <label className="block text-sm font-medium text-slate-700">Professeur
          <select required value={professorId} onChange={(e) => setProfessorId(e.target.value)} className={inputCls}><option value="">Choisir...</option>{professors.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select>
        </label>
        <div className="flex items-end"><button type="submit" disabled={busy} className={primaryBtn}>{busy ? "Ajout..." : "Ajouter la séance"}</button></div>
      </form>
    </ProfessorShell>
  );
}