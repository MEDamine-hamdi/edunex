"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { DefenseRow, InternshipRow, Person, ROLE_LABEL, Role, dateOnly, dateTime, resultBadge } from "@/lib/internships";
import { DefenseDialog } from "./DefenseDialog";
import { Tabs } from "./Tabs";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "./ui";

type Lookups = { students: Person[]; professors: Person[] };
const KINDS = ["PFE", "PFA", "Stage d'été", "Stage d'initiation", "Autre"];
const empty = { id: null as number | null, studentId: "", supervisorId: "", company: "", companySupervisor: "", title: "", kind: "PFE", startDate: "", endDate: "" };
const emptyDefense = { id: null as number | null, internshipId: "", scheduledAt: "", room: "", president: "", rapporteur: "", member: "" };

export function InternshipsAdmin() {
  const [tab, setTab] = useState("interns");
  const [internships, setInternships] = useState<InternshipRow[]>([]);
  const [defenses, setDefenses] = useState<DefenseRow[]>([]);
  const [lookups, setLookups] = useState<Lookups>({ students: [], professors: [] });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(empty);
  const [showForm, setShowForm] = useState(false);
  const [dForm, setDForm] = useState(emptyDefense);
  const [showDForm, setShowDForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [openId, setOpenId] = useState<number | null>(null);

  const fail = (e: unknown) => {
    if (!(e instanceof ApiError)) return;
    setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les stages." : e.status === 409 ? "Cette soutenance ou ce CIN existe déjà." : e.message);
  };

  const load = useCallback(async () => {
    try {
      const [i, d, l] = await Promise.all([
        apiFetch<InternshipRow[]>("/api/admin/internships"),
        apiFetch<DefenseRow[]>("/api/admin/defenses"),
        apiFetch<Lookups>("/api/admin/internships/lookups"),
      ]);
      setInternships(i);
      setDefenses(d);
      setLookups(l);
      setError(null);
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) fail(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function saveInternship(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await apiFetch(form.id ? `/api/admin/internships/${form.id}` : "/api/admin/internships", {
        method: form.id ? "PUT" : "POST",
        body: JSON.stringify({
          studentId: Number(form.studentId), supervisorId: Number(form.supervisorId), company: form.company.trim() || null,
          companySupervisor: form.companySupervisor.trim() || null, title: form.title.trim(), kind: form.kind || null,
          startDate: form.startDate || null, endDate: form.endDate || null,
        }),
      });
      setForm(empty);
      setShowForm(false);
      await load();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) setError("Vérifiez le formulaire (la date de fin ne peut pas précéder la date de début).");
      else fail(err);
    } finally {
      setSaving(false);
    }
  }

  async function removeInternship(i: InternshipRow) {
    if (!window.confirm(`Supprimer le stage de ${i.student.name} ? La soutenance et ses documents seront supprimés aussi.`)) return;
    try {
      await apiFetch<void>(`/api/admin/internships/${i.id}`, { method: "DELETE" });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  function editInternship(i: InternshipRow) {
    setForm({ id: i.id, studentId: String(i.student.id), supervisorId: String(i.supervisor.id), company: i.company ?? "", companySupervisor: i.companySupervisor ?? "", title: i.title, kind: i.kind ?? "", startDate: i.startDate ?? "", endDate: i.endDate ?? "" });
    setShowForm(true);
    setTab("interns");
  }

  async function saveDefense(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const ids = [dForm.president, dForm.rapporteur, dForm.member].filter(Boolean);
    if (new Set(ids).size !== ids.length) {
      setError("Un professeur ne peut apparaître qu'une fois dans le jury.");
      return;
    }
    setSaving(true);
    setError(null);
    const jury: { professorId: number; role: Role }[] = [
      { professorId: Number(dForm.president), role: "PRESIDENT" },
      { professorId: Number(dForm.rapporteur), role: "RAPPORTEUR" },
    ];
    if (dForm.member) jury.push({ professorId: Number(dForm.member), role: "MEMBER" });
    try {
      await apiFetch(dForm.id ? `/api/admin/defenses/${dForm.id}` : "/api/admin/defenses", {
        method: dForm.id ? "PUT" : "POST",
        body: JSON.stringify({ internshipId: Number(dForm.internshipId), scheduledAt: dForm.scheduledAt, room: dForm.room.trim() || null, jury }),
      });
      setDForm(emptyDefense);
      setShowDForm(false);
      await load();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) setError("Le jury doit avoir un président et un rapporteur, et 2 à 3 membres au total.");
      else fail(err);
    } finally {
      setSaving(false);
    }
  }

  function editDefense(d: DefenseRow) {
    const by = (r: Role) => String(d.jury.find((j) => j.role === r)?.professorId ?? "");
    const internship = internships.find((i) => i.defenseId === d.id);
    setDForm({ id: d.id, internshipId: String(internship?.id ?? ""), scheduledAt: d.scheduledAt.slice(0, 16), room: d.room ?? "", president: by("PRESIDENT"), rapporteur: by("RAPPORTEUR"), member: by("MEMBER") });
    setShowDForm(true);
    setTab("jury");
  }

  const profOptions = (
    <>
      <option value="">Choisir...</option>
      {lookups.professors.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
    </>
  );

  return (
    <>
      <Tabs tabs={[{ key: "interns", label: "Stagiaires" }, { key: "jury", label: "Jury" }, { key: "defenses", label: "Soutenances" }]} active={tab} onChange={setTab} />
      <ErrorBox message={error} />
      {loading && <p className="mt-6 text-slate-500">Chargement...</p>}

      {tab === "interns" && (
        <>
          <PageHeader
            title="Stagiaires"
            subtitle="Les étudiants en stage, avec leur encadrant."
            action={<button onClick={() => { setForm(empty); setShowForm((v) => !v); }} className={primaryBtn}>{showForm ? "Fermer" : "+ Nouveau stage"}</button>}
          />
          {showForm && (
            <form onSubmit={saveInternship} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm font-medium text-slate-700">Stagiaire
                  <select required disabled={!!form.id} value={form.studentId} onChange={(e) => setForm({ ...form, studentId: e.target.value })} className={inputCls}>
                    <option value="">Choisir...</option>
                    {lookups.students.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.number})</option>)}
                  </select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Encadrant (professeur)
                  <select required value={form.supervisorId} onChange={(e) => setForm({ ...form, supervisorId: e.target.value })} className={inputCls}>{profOptions}</select>
                </label>
                <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Sujet du stage
                  <input required maxLength={300} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">Entreprise
                  <input maxLength={200} value={form.company} onChange={(e) => setForm({ ...form, company: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">Encadrant en entreprise
                  <input maxLength={200} value={form.companySupervisor} onChange={(e) => setForm({ ...form, companySupervisor: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">Type
                  <select value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })} className={inputCls}>{KINDS.map((k) => <option key={k} value={k}>{k}</option>)}</select>
                </label>
                <div className="grid grid-cols-2 gap-4">
                  <label className="block text-sm font-medium text-slate-700">Début<input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} className={inputCls} /></label>
                  <label className="block text-sm font-medium text-slate-700">Fin<input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} className={inputCls} /></label>
                </div>
              </div>
              <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>{saving ? "Enregistrement..." : "Enregistrer"}</button>
            </form>
          )}
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr><th className="px-5 py-3">Stagiaire</th><th className="px-5 py-3">Encadrant</th><th className="px-5 py-3">Stage</th><th className="px-5 py-3">Période</th><th className="px-5 py-3">Soutenance</th><th className="px-5 py-3 text-right">Actions</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {!loading && internships.length === 0 && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Aucun stage enregistré.</td></tr>}
                {internships.map((i) => (
                  <tr key={i.id} className="align-top">
                    <td className="px-5 py-4"><span className="font-medium text-slate-900">{i.student.name}</span><span className="block text-xs text-slate-500">{i.student.number}</span></td>
                    <td className="px-5 py-4"><span className="font-medium text-slate-800">{i.supervisor.name}</span><a href={`mailto:${i.supervisor.email}`} className="block text-xs text-blue-600 hover:underline">{i.supervisor.email}</a></td>
                    <td className="max-w-xs px-5 py-4 text-slate-700">{i.title}<span className="block text-xs text-slate-500">{[i.kind, i.company].filter(Boolean).join(" · ")}</span></td>
                    <td className="px-5 py-4 text-slate-600">{dateOnly(i.startDate)}<span className="block text-xs text-slate-500">{dateOnly(i.endDate)}</span></td>
                    <td className="px-5 py-4">{i.defenseId ? <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700">{i.defenseAt ? dateTime(i.defenseAt) : "Planifiée"}</span> : <span className="text-slate-400">Aucune</span>}</td>
                    <td className="px-5 py-4 text-right"><div className="flex justify-end gap-2"><button onClick={() => editInternship(i)} className={smallBtn}>Modifier</button><button onClick={() => removeInternship(i)} className={`${smallBtn} text-red-600`}>Supprimer</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === "jury" && (
        <>
          <PageHeader
            title="Jury"
            subtitle="La composition du jury de chaque soutenance : un président, un rapporteur et, souvent, un troisième membre."
            action={<button onClick={() => { setDForm(emptyDefense); setShowDForm((v) => !v); }} className={primaryBtn}>{showDForm ? "Fermer" : "+ Planifier une soutenance"}</button>}
          />
          {showDForm && (
            <form onSubmit={saveDefense} className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="grid gap-4 sm:grid-cols-3">
                <label className="block text-sm font-medium text-slate-700 sm:col-span-3">Stage
                  <select required disabled={!!dForm.id} value={dForm.internshipId} onChange={(e) => setDForm({ ...dForm, internshipId: e.target.value })} className={inputCls}>
                    <option value="">Choisir...</option>
                    {internships.filter((i) => !i.defenseId || i.defenseId === dForm.id).map((i) => <option key={i.id} value={i.id}>{i.student.name} : {i.title}</option>)}
                  </select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Date et heure
                  <input type="datetime-local" required value={dForm.scheduledAt} onChange={(e) => setDForm({ ...dForm, scheduledAt: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Salle
                  <input maxLength={100} value={dForm.room} onChange={(e) => setDForm({ ...dForm, room: e.target.value })} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">Président
                  <select required value={dForm.president} onChange={(e) => setDForm({ ...dForm, president: e.target.value })} className={inputCls}>{profOptions}</select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Rapporteur
                  <select required value={dForm.rapporteur} onChange={(e) => setDForm({ ...dForm, rapporteur: e.target.value })} className={inputCls}>{profOptions}</select>
                </label>
                <label className="block text-sm font-medium text-slate-700">Membre (facultatif)
                  <select value={dForm.member} onChange={(e) => setDForm({ ...dForm, member: e.target.value })} className={inputCls}>{profOptions}</select>
                </label>
              </div>
              <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>{saving ? "Enregistrement..." : "Enregistrer"}</button>
              <p className="mt-3 text-xs text-slate-500">Le stagiaire, l&apos;encadrant et le jury sont prévenus par notification et par e-mail.</p>
            </form>
          )}
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}>
                <tr><th className="px-5 py-3">Stagiaire</th><th className="px-5 py-3">Date</th><th className="px-5 py-3">Président</th><th className="px-5 py-3">Rapporteur</th><th className="px-5 py-3">Membre</th><th className="px-5 py-3 text-right">Action</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {!loading && defenses.length === 0 && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-500">Aucune soutenance planifiée.</td></tr>}
                {defenses.map((d) => (
                  <tr key={d.id}>
                    <td className="px-5 py-3 font-medium text-slate-900">{d.student.name}<span className="block text-xs font-normal text-slate-500">Encadrant : {d.supervisor.name}</span></td>
                    <td className="px-5 py-3 text-slate-600">{dateTime(d.scheduledAt)}<span className="block text-xs text-slate-500">Salle {d.room ?? "-"}</span></td>
                    {(["PRESIDENT", "RAPPORTEUR", "MEMBER"] as Role[]).map((r) => <td key={r} className="px-5 py-3 text-slate-700">{d.jury.find((j) => j.role === r)?.name ?? "-"}</td>)}
                    <td className="px-5 py-3 text-right"><button onClick={() => editDefense(d)} className={smallBtn}>Modifier</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === "defenses" && (
        <>
          <PageHeader title="Soutenances" subtitle="Cliquez sur une soutenance pour voir le résultat, le jury, le rapport final et la feuille de résultat." />
          <div className="mt-6 space-y-3">
            {!loading && defenses.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-500">Aucune soutenance pour le moment.</p>}
            {defenses.map((d) => {
              const badge = resultBadge(d.grade, d.status);
              return (
                <button key={d.id} onClick={() => setOpenId(d.id)} className="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-5 text-start shadow-sm transition hover:border-blue-300">
                  <div className="min-w-0">
                    <p className="font-semibold text-slate-900">{d.student.name}</p>
                    <p className="truncate text-sm text-slate-600">{d.title}</p>
                    <p className="mt-1 text-xs text-slate-500">{dateTime(d.scheduledAt)} · Salle {d.room ?? "-"} · Jury : {d.jury.map((j) => `${ROLE_LABEL[j.role]} ${j.name}`).join(", ")}</p>
                  </div>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${badge.cls}`}>{badge.text}{d.mention ? ` · ${d.mention}` : ""}</span>
                </button>
              );
            })}
          </div>
        </>
      )}
      {openId && <DefenseDialog id={openId} canEdit onClose={() => setOpenId(null)} onChanged={load} />}
    </>
  );
}