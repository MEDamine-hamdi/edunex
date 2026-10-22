"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Tabs } from "@/components/Tabs";
import { ErrorBox, IdPicker, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Group = { id: number; name: string; students: number };
type ProfRef = { id: number; name: string };
type SubjectRow = { id: number; name: string; code: string; professors: ProfRef[]; custom: boolean };
type ProfessorRow = { id: number; name: string; email: string; subjects: string[] };
type StudentRow = { enrollmentId: number; studentId: number; name: string; studentNumber: string; email: string; groupId: number | null; groupName: string | null };
type Overview = { id: number; name: string; level: string | null; academicYear: string; groups: Group[]; subjects: SubjectRow[]; professors: ProfessorRow[]; students: StudentRow[] };
type Candidate = { id: number; name: string; studentNumber: string; email: string };
type SubjectOption = { id: number; name: string; code: string };
type Tools = { ov: Overview; reload: () => Promise<void>; fail: (e: unknown) => void };

const API = (id: number | string) => `/api/admin/class-management/${id}`;

function errorText(e: unknown) {
  if (!(e instanceof ApiError)) return "Une erreur est survenue.";
  return e.status === 403 ? "Vous n'avez pas la permission de gérer les classes." : e.message;
}

function StudentsTab({ ov, reload, fail }: Tools) {
  const [adding, setAdding] = useState(false);
  const [q, setQ] = useState("");
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [picked, setPicked] = useState<number[]>([]);
  const [group, setGroup] = useState("");
  const [busy, setBusy] = useState(false);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    if (!adding) return;
    const t = setTimeout(() => {
      apiFetch<Candidate[]>(`${API(ov.id)}/candidates?q=${encodeURIComponent(q)}`).then(setCandidates).catch(fail);
    }, 250);
    return () => clearTimeout(t);
  }, [adding, q, ov.id, ov.students.length, fail]);

  async function add() {
    setBusy(true);
    setInfo(null);
    try {
      const r = await apiFetch<{ added: number; skipped: number }>(`${API(ov.id)}/students`, {
        method: "POST",
        body: JSON.stringify({ studentIds: picked, groupId: group ? Number(group) : null }),
      });
      setInfo(`${r.added} étudiant(s) ajouté(s)${r.skipped ? `, ${r.skipped} ignoré(s)` : ""}.`);
      setPicked([]);
      await reload();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  async function changeGroup(s: StudentRow, value: string) {
    try {
      await apiFetch(`${API(ov.id)}/students/${s.studentId}/group`, { method: "PUT", body: JSON.stringify({ groupId: value ? Number(value) : null }) });
      await reload();
    } catch (e) {
      fail(e);
    }
  }

  async function remove(s: StudentRow) {
    if (!window.confirm(`Retirer ${s.name} de la classe ?`)) return;
    try {
      await apiFetch<void>(`${API(ov.id)}/students/${s.studentId}`, { method: "DELETE" });
      await reload();
    } catch (e) {
      fail(e);
    }
  }

  return (
    <div>
      <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Étudiants de la classe ({ov.students.length})</h2>
        <button onClick={() => setAdding((v) => !v)} className={primaryBtn}>{adding ? "Fermer" : "+ Ajouter des étudiants"}</button>
      </div>
      {info && <p className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {adding && (
        <section className="mt-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap gap-3">
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher un nom, un e-mail ou un numéro" className={`${inputCls} mt-0 min-w-64 flex-1`} />
            <select value={group} onChange={(e) => setGroup(e.target.value)} className={`${inputCls} mt-0 w-56`}>
              <option value="">Sans groupe</option>
              {ov.groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </select>
          </div>
          <ul className="mt-4 max-h-72 space-y-1 overflow-y-auto">
            {candidates.length === 0 && <li className="px-2 py-3 text-sm text-slate-500">Aucun étudiant disponible.</li>}
            {candidates.map((c) => (
              <li key={c.id}>
                <label className="flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm hover:bg-slate-50">
                  <input type="checkbox" className="h-4 w-4 accent-blue-600" checked={picked.includes(c.id)}
                    onChange={() => setPicked((p) => (p.includes(c.id) ? p.filter((x) => x !== c.id) : [...p, c.id]))} />
                  <span className="font-medium text-slate-900">{c.name}</span>
                  <span className="text-xs text-slate-500">{c.studentNumber} · {c.email}</span>
                </label>
              </li>
            ))}
          </ul>
          <button onClick={add} disabled={busy || picked.length === 0} className={`mt-4 ${primaryBtn}`}>
            {busy ? "Ajout..." : `Ajouter (${picked.length})`}
          </button>
        </section>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}>
            <tr>
              <th className="px-5 py-3">Étudiant</th>
              <th className="px-5 py-3">Email</th>
              <th className="px-5 py-3">Groupe</th>
              <th className="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {ov.students.length === 0 && <tr><td colSpan={4} className="px-5 py-8 text-center text-slate-500">Aucun étudiant inscrit.</td></tr>}
            {ov.students.map((s) => (
              <tr key={s.enrollmentId}>
                <td className="px-5 py-3">
                  <span className="font-medium text-slate-900">{s.name}</span>
                  <span className="block text-xs text-slate-500">{s.studentNumber}</span>
                </td>
                <td className="px-5 py-3 text-slate-600">{s.email}</td>
                <td className="px-5 py-3">
                  <select value={s.groupId ?? ""} onChange={(e) => changeGroup(s, e.target.value)} className={`${inputCls} mt-0 w-48`}>
                    <option value="">Sans groupe</option>
                    {ov.groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
                  </select>
                </td>
                <td className="px-5 py-3 text-right"><button onClick={() => remove(s)} className={`${smallBtn} text-red-600`}>Retirer</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ProfessorsTab({ ov, reload, fail }: Tools) {
  const [options, setOptions] = useState<ProfRef[]>([]);
  const [editing, setEditing] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    apiFetch<ProfRef[]>("/api/admin/class-management/professors").then(setOptions).catch(fail);
  }, [fail]);

  async function save(subjectId: number, ids: number[]) {
    setBusy(true);
    try {
      await apiFetch(`${API(ov.id)}/subjects/${subjectId}/professors`, { method: "PUT", body: JSON.stringify({ ids }) });
      setEditing(null);
      await reload();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mt-6 space-y-8">
      <section>
        <h2 className="text-lg font-semibold">Professeurs de la classe ({ov.professors.length})</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {ov.professors.length === 0 && <p className="text-sm text-slate-500">Aucun professeur n&apos;enseigne dans cette classe pour le moment.</p>}
          {ov.professors.map((p) => (
            <div key={p.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <p className="font-semibold text-slate-900">{p.name}</p>
              <p className="text-xs text-slate-500">{p.email}</p>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {p.subjects.map((s) => <span key={s} className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700">{s}</span>)}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2 className="text-lg font-semibold">Affectations par matière</h2>
        <p className="mt-1 text-sm text-slate-500">
          Choisissez qui enseigne chaque matière dans cette classe. Sans choix explicite, ce sont les professeurs de la matière qui s&apos;appliquent.
        </p>
        <div className="mt-4 space-y-4">
          {ov.subjects.length === 0 && <p className="text-sm text-slate-500">Cette classe n&apos;a pas encore de matière (onglet Matières).</p>}
          {ov.subjects.map((s) => (
            <div key={s.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-900">{s.name} <span className="text-xs font-normal text-slate-500">({s.code})</span></p>
                  <p className="mt-1 text-sm text-slate-600">
                    {s.professors.length === 0 ? "Aucun professeur." : s.professors.map((p) => p.name).join(", ")}
                    {!s.custom && s.professors.length > 0 && <span className="ms-2 text-xs text-slate-400">(par défaut)</span>}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => setEditing(editing === s.id ? null : s.id)} className={smallBtn}>{editing === s.id ? "Fermer" : "Modifier"}</button>
                  {s.custom && <button onClick={() => save(s.id, [])} disabled={busy} className={smallBtn}>Rétablir par défaut</button>}
                </div>
              </div>
              {editing === s.id && (
                <div className="mt-4">
                  <IdPicker options={options.map((o) => ({ id: o.id, label: o.name }))} selected={s.professors.map((p) => p.id)}
                    onSave={(ids) => save(s.id, ids)} saving={busy} emptyText="Aucun professeur actif." />
                </div>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function SubjectsTab({ ov, reload, fail }: Tools) {
  const [options, setOptions] = useState<SubjectOption[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    apiFetch<SubjectOption[]>("/api/admin/subjects").then(setOptions).catch(fail);
  }, [fail]);

  async function save(ids: number[]) {
    setBusy(true);
    try {
      await apiFetch(`/api/admin/classes/${ov.id}/subjects`, { method: "PUT", body: JSON.stringify({ ids }) });
      await reload();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mt-6">
      <h2 className="text-lg font-semibold">Matières suivies par la classe</h2>
      <div className="mt-4">
        <IdPicker options={options.map((o) => ({ id: o.id, label: `${o.name} (${o.code})` }))} selected={ov.subjects.map((s) => s.id)}
          onSave={save} saving={busy} emptyText="Aucune matière n'existe encore." />
      </div>
    </div>
  );
}

function GroupsTab({ ov, reload, fail }: Tools) {
  const [name, setName] = useState("");

  async function create() {
    if (!name.trim()) return;
    try {
      await apiFetch(`/api/admin/classes/${ov.id}/sections`, { method: "POST", body: JSON.stringify({ name: name.trim() }) });
      setName("");
      await reload();
    } catch (e) {
      fail(e);
    }
  }

  async function rename(g: Group) {
    const value = window.prompt("Nouveau nom du groupe", g.name);
    if (!value || !value.trim() || value.trim() === g.name) return;
    try {
      await apiFetch(`${API(ov.id)}/groups/${g.id}`, { method: "PUT", body: JSON.stringify({ name: value.trim() }) });
      await reload();
    } catch (e) {
      fail(e);
    }
  }

  async function remove(g: Group) {
    if (!window.confirm(`Supprimer le groupe « ${g.name} » ? Ses étudiants resteront dans la classe, sans groupe.`)) return;
    try {
      await apiFetch<void>(`${API(ov.id)}/groups/${g.id}`, { method: "DELETE" });
      await reload();
    } catch (e) {
      fail(e);
    }
  }

  return (
    <div className="mt-6">
      <h2 className="text-lg font-semibold">Groupes de la classe</h2>
      <div className="mt-4 flex max-w-xl gap-3">
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Nom du groupe (ex. Groupe B)" className={`${inputCls} mt-0`} />
        <button onClick={create} className={primaryBtn}>Ajouter</button>
      </div>
      <ul className="mt-5 space-y-2">
        {ov.groups.length === 0 && <li className="text-sm text-slate-500">Aucun groupe.</li>}
        {ov.groups.map((g) => (
          <li key={g.id} className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 shadow-sm">
            <span className="font-medium text-slate-900">{g.name} <span className="ms-2 text-xs font-normal text-slate-500">{g.students} étudiant(s)</span></span>
            <div className="flex gap-2">
              <button onClick={() => rename(g)} className={smallBtn}>Renommer</button>
              <button onClick={() => remove(g)} className={`${smallBtn} text-red-600`}>Supprimer</button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function ManageClassPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [ov, setOv] = useState<Overview | null>(null);
  const [tab, setTab] = useState("students");
  const [error, setError] = useState<string | null>(null);
  const fail = useCallback((e: unknown) => setError(errorText(e)), []);

  const reload = useCallback(async () => {
    try {
      setOv(await apiFetch<Overview>(`${API(id)}/overview`));
      setError(null);
    } catch (e) {
      fail(e);
    }
  }, [id, fail]);

  useEffect(() => {
    reload();
  }, [reload]);

  return (
    <AdminShell>
      <Link href="/admin/classes" className="text-sm font-medium text-blue-600 hover:underline">← Retour aux classes</Link>
      <ErrorBox message={error} />
      {!ov && !error && <p className="mt-6 text-slate-500">Chargement...</p>}
      {ov && (
        <>
          <div className="mt-3">
            <h1 className="text-2xl font-extrabold tracking-tight">{ov.name}</h1>
            <p className="mt-1 text-sm text-slate-500">
              {ov.level ?? "-"} · {ov.academicYear} · {ov.students.length} étudiant(s) · {ov.professors.length} professeur(s) · {ov.groups.length} groupe(s)
            </p>
          </div>
          <div className="mt-6">
            <Tabs
              tabs={[
                { key: "students", label: "Étudiants" },
                { key: "professors", label: "Professeurs" },
                { key: "subjects", label: "Matières" },
                { key: "groups", label: "Groupes" },
              ]}
              active={tab}
              onChange={setTab}
            />
          </div>
          {tab === "students" && <StudentsTab ov={ov} reload={reload} fail={fail} />}
          {tab === "professors" && <ProfessorsTab ov={ov} reload={reload} fail={fail} />}
          {tab === "subjects" && <SubjectsTab ov={ov} reload={reload} fail={fail} />}
          {tab === "groups" && <GroupsTab ov={ov} reload={reload} fail={fail} />}
        </>
      )}
    </AdminShell>
  );
}