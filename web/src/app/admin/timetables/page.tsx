"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { DAYS, WeekGrid } from "@/components/WeekGrid";
import { ApiError, apiFetch } from "@/lib/api";
import { ErrorBox, PageHeader, TextField, inputCls, primaryBtn, smallBtn } from "@/components/ui";
import type { Session, Timetable } from "@/lib/types";

type ClassItem = { id: number; name: string; subjects: { id: number; name: string; code: string }[] };
type SubjectItem = {
  id: number;
  name: string;
  code: string;
  professors: { id: number; firstName: string; lastName: string }[];
};

const emptyForm = {
  dayOfWeek: "MONDAY",
  startTime: "",
  endTime: "",
  room: "",
  classId: "",
  subjectId: "",
  professorId: "",
};

function explain(err: ApiError): string {
  const m = err.message;
  if (m.includes("professor already")) return "Conflit : ce professeur a déjà une séance à ce créneau.";
  if (m.includes("room is already")) return "Conflit : cette salle est déjà occupée à ce créneau.";
  if (m.includes("class already")) return "Conflit : cette classe a déjà une séance à ce créneau.";
  if (m.includes("End time")) return "L'heure de fin doit être après l'heure de début.";
  if (m.includes("not taught")) return "Cette matière n'est pas enseignée dans cette classe.";
  if (m.includes("not assigned")) return "Ce professeur n'est pas affecté à cette matière.";
  if (m.includes("already exists")) return "Un emploi du temps existe déjà pour cette année et ce semestre.";
  return m;
}

export default function TimetablesPage() {
  const [timetables, setTimetables] = useState<Timetable[]>([]);
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [classFilter, setClassFilter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [showTtForm, setShowTtForm] = useState(false);
  const [ttForm, setTtForm] = useState({ academicYear: "", semester: "" });

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);

  const loadBase = useCallback(async () => {
    try {
      const [t, c, s] = await Promise.all([
        apiFetch<Timetable[]>("/api/admin/timetables"),
        apiFetch<ClassItem[]>("/api/admin/classes"),
        apiFetch<SubjectItem[]>("/api/admin/subjects"),
      ]);
      setTimetables(t);
      setClasses(c);
      setSubjects(s);
      setSelectedId((cur) => cur ?? t[0]?.id ?? null);
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadSessions = useCallback(async (id: number) => {
    try {
      setSessions(await apiFetch<Session[]>(`/api/admin/timetables/${id}/sessions`));
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.message);
    }
  }, []);

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  useEffect(() => {
    if (selectedId) loadSessions(selectedId);
    else setSessions([]);
  }, [selectedId, loadSessions]);

  const subjectOptions = useMemo(
    () => classes.find((c) => String(c.id) === form.classId)?.subjects ?? [],
    [classes, form.classId]
  );
  const professorOptions = useMemo(
    () => subjects.find((s) => String(s.id) === form.subjectId)?.professors ?? [],
    [subjects, form.subjectId]
  );
  const visible = classFilter ? sessions.filter((s) => String(s.classId) === classFilter) : sessions;

  async function createTimetable(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await apiFetch<Timetable>("/api/admin/timetables", {
        method: "POST",
        body: JSON.stringify({ academicYear: ttForm.academicYear.trim(), semester: ttForm.semester.trim() }),
      });
      setTimetables((prev) => [...prev, created]);
      setSelectedId(created.id);
      setTtForm({ academicYear: "", semester: "" });
      setShowTtForm(false);
    } catch (err) {
      if (err instanceof ApiError) setError(explain(err));
    } finally {
      setSaving(false);
    }
  }

  function openNew() {
    setEditingId(null);
    setForm(emptyForm);
    setFormError(null);
    setShowForm(true);
  }

  function startEdit(s: Session) {
    setEditingId(s.id);
    setForm({
      dayOfWeek: s.dayOfWeek,
      startTime: s.startTime.slice(0, 5),
      endTime: s.endTime.slice(0, 5),
      room: s.room ?? "",
      classId: String(s.classId),
      subjectId: String(s.subjectId),
      professorId: String(s.professorId),
    });
    setFormError(null);
    setShowForm(true);
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!selectedId) return;
    setSaving(true);
    setFormError(null);
    const body = JSON.stringify({
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      room: form.room.trim() || null,
      classId: Number(form.classId),
      subjectId: Number(form.subjectId),
      professorId: Number(form.professorId),
    });
    try {
      if (editingId) {
        await apiFetch<Session>(`/api/admin/sessions/${editingId}`, { method: "PUT", body });
      } else {
        await apiFetch<Session>(`/api/admin/timetables/${selectedId}/sessions`, { method: "POST", body });
      }
      await loadSessions(selectedId);
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
    } catch (err) {
      if (err instanceof ApiError) setFormError(explain(err));
    } finally {
      setSaving(false);
    }
  }

  async function remove(s: Session) {
    if (!window.confirm(`Supprimer la séance ${s.subjectCode} du ${s.startTime.slice(0, 5)} ?`)) return;
    setError(null);
    try {
      await apiFetch<void>(`/api/admin/sessions/${s.id}`, { method: "DELETE" });
      setSessions((prev) => prev.filter((x) => x.id !== s.id));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  return (
    <AdminShell>
      <PageHeader
        title="Emplois du temps"
        subtitle="Planifiez les séances. Les conflits de professeur, de salle et de classe sont détectés automatiquement."
        action={
          <div className="flex gap-3">
            <button onClick={() => setShowTtForm((v) => !v)} className={smallBtn}>
              {showTtForm ? "Fermer" : "+ Nouvel emploi du temps"}
            </button>
            {selectedId && (
              <button onClick={showForm ? () => setShowForm(false) : openNew} className={primaryBtn}>
                {showForm ? "Fermer" : "+ Nouvelle séance"}
              </button>
            )}
          </div>
        }
      />
      <ErrorBox message={error} />

      {showTtForm && (
        <form onSubmit={createTimetable} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Année universitaire (ex. 2026-2027)" value={ttForm.academicYear} onChange={(e) => setTtForm({ ...ttForm, academicYear: e.target.value })} required />
            <TextField label="Semestre (ex. S1)" value={ttForm.semester} onChange={(e) => setTtForm({ ...ttForm, semester: e.target.value })} required />
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>Créer</button>
        </form>
      )}

      {!loading && timetables.length === 0 && (
        <p className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Aucun emploi du temps. Créez-en un pour commencer.
        </p>
      )}

      {timetables.length > 0 && (
        <div className="mt-6 flex flex-wrap gap-4">
          <label className="text-sm font-medium text-slate-700">
            Emploi du temps
            <select
              value={selectedId ?? ""}
              onChange={(e) => setSelectedId(Number(e.target.value))}
              className={`${inputCls} w-64`}
            >
              {timetables.map((t) => (
                <option key={t.id} value={t.id}>{t.academicYear} - {t.semester}</option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-slate-700">
            Filtrer par classe
            <select value={classFilter} onChange={(e) => setClassFilter(e.target.value)} className={`${inputCls} w-64`}>
              <option value="">Toutes les classes</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </label>
        </div>
      )}

      {showForm && selectedId && (
        <form onSubmit={submit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold">{editingId ? "Modifier la séance" : "Nouvelle séance"}</h2>
          <ErrorBox message={formError} />
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <label className="block text-sm font-medium text-slate-700">
              Jour
              <select value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })} className={inputCls}>
                {DAYS.map((d) => (
                  <option key={d.key} value={d.key}>{d.label}</option>
                ))}
              </select>
            </label>
            <TextField label="Début" type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required />
            <TextField label="Fin" type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required />
            <TextField label="Salle" value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
            <label className="block text-sm font-medium text-slate-700">
              Classe
              <select
                required
                value={form.classId}
                onChange={(e) => setForm({ ...form, classId: e.target.value, subjectId: "", professorId: "" })}
                className={inputCls}
              >
                <option value="">Choisir...</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Matière
              <select
                required
                value={form.subjectId}
                disabled={!form.classId}
                onChange={(e) => setForm({ ...form, subjectId: e.target.value, professorId: "" })}
                className={inputCls}
              >
                <option value="">{form.classId ? "Choisir..." : "Choisissez d'abord une classe"}</option>
                {subjectOptions.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.code})</option>
                ))}
              </select>
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Professeur
              <select
                required
                value={form.professorId}
                disabled={!form.subjectId}
                onChange={(e) => setForm({ ...form, professorId: e.target.value })}
                className={inputCls}
              >
                <option value="">{form.subjectId ? "Choisir..." : "Choisissez d'abord une matière"}</option>
                {professorOptions.map((p) => (
                  <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>
                ))}
              </select>
            </label>
          </div>
          <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
            {saving ? "Enregistrement..." : editingId ? "Enregistrer" : "Ajouter la séance"}
          </button>
        </form>
      )}

      {selectedId && <WeekGrid sessions={visible} showClass showProfessor onEdit={startEdit} onDelete={remove} />}
    </AdminShell>
  );
}