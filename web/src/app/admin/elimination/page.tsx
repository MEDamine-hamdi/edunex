"use client";

import { useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Value = { maxAbsences: number | null; latesPerAbsence: number | null };
type SubjectPolicy = { subjectId: number; name: string; code: string; override: Value | null };
type Policy = { defaultPolicy: Value | null; subjects: SubjectPolicy[] };
type Row = { id: number; studentName: string; studentNumber: string; subjectName: string; subjectCode: string; absences: number; max: number; eliminatedAt: string; waived: boolean; waivedBy: string | null; waiveReason: string | null };

export default function AdminEliminationPage() {
  const [tab, setTab] = useState<"policy" | "list">("policy");
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  const [defMax, setDefMax] = useState("");
  const [defLates, setDefLates] = useState("0");
  const [edits, setEdits] = useState<Record<number, { max: string; lates: string }>>({});
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [p, r] = await Promise.all([apiFetch<Policy>("/api/admin/elimination/policy"), apiFetch<Row[]>("/api/admin/elimination")]);
      setPolicy(p);
      setRows(r);
      setDefMax(p.defaultPolicy?.maxAbsences ? String(p.defaultPolicy.maxAbsences) : "");
      setDefLates(String(p.defaultPolicy?.latesPerAbsence ?? 0));
      setEdits(Object.fromEntries(p.subjects.map((s) => [s.subjectId, { max: s.override?.maxAbsences ? String(s.override.maxAbsences) : "", lates: String(s.override?.latesPerAbsence ?? 0) }])));
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) setError(e.status === 403 ? "Vous n'avez pas la permission de gérer les éliminations." : e.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function save(path: string, max: string, lates: string) {
    setError(null);
    setInfo(null);
    try {
      await apiFetch(path, { method: "PUT", body: JSON.stringify({ maxAbsences: max ? Number(max) : 0, latesPerAbsence: Number(lates || 0) }) });
      setInfo("Seuil enregistré : les éliminations sont recalculées.");
      await load();
    } catch (e) {
      if (e instanceof ApiError) setError(e.status === 400 ? "Valeurs invalides (absences : 1 à 99, retards : 0 à 20)." : e.message);
    }
  }

  async function act(path: string, body?: unknown) {
    try {
      await apiFetch(path, { method: "POST", body: body ? JSON.stringify(body) : undefined });
      await load();
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  }

  return (
    <AdminShell>
      <PageHeader title="Éliminations" subtitle="Fixez le nombre d'absences autorisées. Un étudiant qui le dépasse dans une matière en est éliminé : il est prévenu, ainsi que les professeurs." />
      <div className="mt-6 flex gap-1 border-b border-slate-200">
        {(["policy", "list"] as const).map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`-mb-px border-b-2 px-4 py-2.5 text-sm font-medium ${tab === t ? "border-blue-600 text-blue-700" : "border-transparent text-slate-500"}`}>{t === "policy" ? "Seuils" : `Éliminations (${rows.filter((r) => !r.waived).length})`}</button>
        ))}
      </div>
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      {tab === "policy" && policy && (
        <>
          <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold">Seuil par défaut (toutes les matières)</h2>
            <p className="mt-1 text-sm text-slate-500">Laissez vide pour désactiver l&apos;élimination partout où une matière n&apos;a pas son propre seuil.</p>
            <div className="mt-4 flex flex-wrap items-end gap-4">
              <label className="block text-sm font-medium text-slate-700">Absences autorisées<input type="number" min={1} max={99} value={defMax} onChange={(e) => setDefMax(e.target.value)} className={`${inputCls} w-40`} /></label>
              <label className="block text-sm font-medium text-slate-700">Retards = 1 absence (0 = ignorés)<input type="number" min={0} max={20} value={defLates} onChange={(e) => setDefLates(e.target.value)} className={`${inputCls} w-40`} /></label>
              <button onClick={() => save("/api/admin/elimination/policy/default", defMax, defLates)} className={primaryBtn}>Enregistrer</button>
            </div>
          </section>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}><tr><th className="px-5 py-3">Matière</th><th className="px-5 py-3">Seuil propre</th><th className="px-5 py-3">Retards = 1 absence</th><th className="px-5 py-3 text-right">Actions</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {policy.subjects.map((s) => {
                  const e = edits[s.subjectId] ?? { max: "", lates: "0" };
                  return (
                    <tr key={s.subjectId}>
                      <td className="px-5 py-3 font-medium text-slate-900">{s.name}<span className="block text-xs font-normal text-slate-500">{s.code}{s.override ? "" : " · seuil par défaut"}</span></td>
                      <td className="px-5 py-3"><input type="number" min={1} max={99} value={e.max} placeholder="défaut" onChange={(ev) => setEdits({ ...edits, [s.subjectId]: { ...e, max: ev.target.value } })} className={`${inputCls} mt-0 w-28`} /></td>
                      <td className="px-5 py-3"><input type="number" min={0} max={20} value={e.lates} onChange={(ev) => setEdits({ ...edits, [s.subjectId]: { ...e, lates: ev.target.value } })} className={`${inputCls} mt-0 w-28`} /></td>
                      <td className="px-5 py-3 text-right"><div className="flex justify-end gap-2">
                        <button onClick={() => save(`/api/admin/elimination/policy/subjects/${s.subjectId}`, e.max, e.lates)} disabled={!e.max} className={smallBtn}>Enregistrer</button>
                        {s.override && <button onClick={async () => { await apiFetch(`/api/admin/elimination/policy/subjects/${s.subjectId}`, { method: "DELETE" }); await load(); }} className={smallBtn}>Retirer</button>}
                      </div></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === "list" && (
        <div className={tableWrap}>
          <table className="w-full text-left text-sm">
            <thead className={theadCls}><tr><th className="px-5 py-3">Étudiant</th><th className="px-5 py-3">Matière</th><th className="px-5 py-3">Absences</th><th className="px-5 py-3">Statut</th><th className="px-5 py-3 text-right">Action</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {rows.length === 0 && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-500">Aucun étudiant éliminé.</td></tr>}
              {rows.map((r) => (
                <tr key={r.id}>
                  <td className="px-5 py-3 font-medium text-slate-900">{r.studentName}<span className="block text-xs font-normal text-slate-500">{r.studentNumber}</span></td>
                  <td className="px-5 py-3 text-slate-700">{r.subjectName}<span className="block text-xs text-slate-500">{r.subjectCode}</span></td>
                  <td className="px-5 py-3 text-slate-700">{r.absences} / {r.max}</td>
                  <td className="px-5 py-3">{r.waived ? <span className="text-xs text-amber-800">Levée par {r.waivedBy}{r.waiveReason ? ` : ${r.waiveReason}` : ""}</span> : <span className="rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700">Éliminé(e)</span>}</td>
                  <td className="px-5 py-3 text-right">{r.waived ? <button onClick={() => act(`/api/admin/elimination/${r.id}/restore`)} className={smallBtn}>Rétablir</button> : <button onClick={() => { const reason = window.prompt("Motif de la levée de l'élimination (facultatif)"); if (reason !== null) act(`/api/admin/elimination/${r.id}/waive`, { reason }); }} className={smallBtn}>Lever l&apos;élimination</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </AdminShell>
  );
}