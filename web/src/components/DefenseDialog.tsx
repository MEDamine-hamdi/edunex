"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { downloadFromUrl } from "@/lib/download";
import { DefenseDetail, ROLE_LABEL, dateOnly, dateTime } from "@/lib/internships";
import { ErrorBox, inputCls, primaryBtn, smallBtn } from "./ui";

function Person({ title, name, email, extra }: { title: string; name: string; email: string; extra?: string }) {
  return (
    <div className="rounded-xl border border-slate-200 p-3 text-sm">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">{title}</p>
      <p className="mt-1 font-medium text-slate-900">{name}</p>
      <a href={`mailto:${email}`} className="text-xs text-blue-600 hover:underline">{email}</a>
      {extra && <p className="text-xs text-slate-500">{extra}</p>}
    </div>
  );
}

/** Popup of one soutenance. With canEdit, the administration can enter the result and upload the two documents. */
export function DefenseDialog({ id, canEdit, onClose, onChanged }: { id: number; canEdit: boolean; onClose: () => void; onChanged?: () => void }) {
  const [d, setD] = useState<DefenseDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [grade, setGrade] = useState("");
  const [comments, setComments] = useState("");
  const [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const reportInput = useRef<HTMLInputElement>(null);
  const recordInput = useRef<HTMLInputElement>(null);

  const apply = useCallback((detail: DefenseDetail) => {
    setD(detail);
    setGrade(detail.grade != null ? String(detail.grade) : "");
    setComments(detail.comments ?? "");
  }, []);

  useEffect(() => {
    apiFetch<DefenseDetail>(`/api/defenses/${id}`).then(apply).catch((e) => e instanceof ApiError && setError(e.message));
  }, [id, apply]);

  // a scanned record that is an image is shown in the popup
  useEffect(() => {
    if (!d?.hasRecord || !/\.(png|jpe?g)$/i.test(d.recordName ?? "")) {
      setPreview(null);
      return;
    }
    let url: string | null = null;
    let off = false;
    fetchBlobUrl(`/api/defenses/${id}/record`)
      .then((u) => {
        if (off) URL.revokeObjectURL(u);
        else {
          url = u;
          setPreview(u);
        }
      })
      .catch(() => {});
    return () => {
      off = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [d?.hasRecord, d?.recordName, id]);

  async function open(kind: "report" | "record") {
    try {
      const url = await fetchBlobUrl(`/api/defenses/${id}/${kind}`);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch {
      setError("Ouverture impossible.");
    }
  }

  async function saveResult(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      apply(await apiFetch<DefenseDetail>(`/api/admin/defenses/${id}/result`, { method: "PUT", body: JSON.stringify({ grade: Number(grade), comments: comments.trim() || null }) }));
      onChanged?.();
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 400 ? "La note doit être comprise entre 0 et 20." : err.message);
    } finally {
      setBusy(false);
    }
  }

  async function upload(kind: "report" | "record", file: File | undefined) {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      apply(await apiFetch<DefenseDetail>(`/api/admin/defenses/${id}/${kind}`, { method: "POST", body: fd }));
      onChanged?.();
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 400 ? (kind === "report" ? "Le rapport doit être un PDF valide (25 Mo maximum)." : "Utilisez un PDF, PNG ou JPG valide (15 Mo maximum).") : err.message);
    } finally {
      setBusy(false);
      if (reportInput.current) reportInput.current.value = "";
      if (recordInput.current) recordInput.current.value = "";
    }
  }

  async function remove(kind: "report" | "record") {
    if (!window.confirm("Supprimer ce document ?")) return;
    try {
      apply(await apiFetch<DefenseDetail>(`/api/admin/defenses/${id}/${kind}`, { method: "DELETE" }));
      onChanged?.();
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  const doc = (kind: "report" | "record", title: string, has: boolean, name: string | null, accept: string) => (
    <div className="rounded-xl border border-slate-200 p-4">
      <p className="text-sm font-semibold text-slate-900">{title}</p>
      {has ? (
        <>
          <p className="mt-1 truncate text-xs text-slate-500">{name}</p>
          <div className="mt-3 flex flex-wrap gap-2">
            <button onClick={() => open(kind)} className={smallBtn}>Ouvrir</button>
            <button onClick={() => downloadFromUrl(`/api/defenses/${id}/${kind}`, name ?? "document").catch(() => setError("Téléchargement impossible."))} className={smallBtn}>Télécharger</button>
            {canEdit && <button onClick={() => remove(kind)} className={`${smallBtn} text-red-600`}>Supprimer</button>}
          </div>
        </>
      ) : (
        <p className="mt-1 text-xs text-slate-400">Aucun document pour le moment.</p>
      )}
      {canEdit && (
        <div className="mt-3">
          <input ref={kind === "report" ? reportInput : recordInput} type="file" hidden accept={accept} onChange={(e) => upload(kind, e.target.files?.[0])} />
          <button onClick={() => (kind === "report" ? reportInput : recordInput).current?.click()} disabled={busy} className={smallBtn}>
            {has ? "Remplacer" : "Envoyer le document"}
          </button>
        </div>
      )}
    </div>
  );

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4" onClick={onClose}>
      <div className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Soutenance</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">{d?.title ?? "..."}</h2>
          </div>
          <button onClick={onClose} className={smallBtn}>Fermer</button>
        </div>
        <ErrorBox message={error} />

        {d && (
          <div className="mt-5 space-y-6">
            <section className="rounded-2xl bg-slate-50 p-4">
              <div className="flex flex-wrap items-center gap-x-8 gap-y-3">
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Résultat</p>
                  {d.status === "DONE" && d.grade != null ? (
                    <p className="mt-1 text-2xl font-extrabold text-slate-900">
                      {d.grade}<span className="text-sm font-medium text-slate-500"> / 20</span>
                    </p>
                  ) : (
                    <p className="mt-1 text-sm text-slate-500">Pas encore saisi</p>
                  )}
                </div>
                {d.mention && (
                  <>
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Mention</p>
                      <p className="mt-1 text-sm font-semibold text-slate-900">{d.mention}</p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${d.decision === "Admis" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"}`}>{d.decision}</span>
                  </>
                )}
                <div className="ms-auto text-sm text-slate-600">
                  <p>{dateTime(d.scheduledAt)}</p>
                  <p className="text-xs text-slate-500">Salle {d.room ?? "-"}</p>
                </div>
              </div>
              {d.comments && <p className="mt-3 whitespace-pre-line text-sm text-slate-700">{d.comments}</p>}
            </section>

            {canEdit && (
              <form onSubmit={saveResult} className="grid gap-3 rounded-2xl border border-slate-200 p-4 sm:grid-cols-[8rem_1fr_auto] sm:items-end">
                <label className="block text-sm font-medium text-slate-700">
                  Note (sur 20)
                  <input type="number" min={0} max={20} step={0.25} required value={grade} onChange={(e) => setGrade(e.target.value)} className={inputCls} />
                </label>
                <label className="block text-sm font-medium text-slate-700">
                  Appréciation
                  <input maxLength={2000} value={comments} onChange={(e) => setComments(e.target.value)} className={inputCls} />
                </label>
                <button type="submit" disabled={busy} className={primaryBtn}>Enregistrer le résultat</button>
              </form>
            )}

            <section>
              <h3 className="text-sm font-semibold text-slate-900">Stagiaire et encadrement</h3>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Person title="Stagiaire" name={d.student.name} email={d.student.email} extra={d.student.number ?? undefined} />
                <Person title="Encadrant" name={d.supervisor.name} email={d.supervisor.email} />
              </div>
            </section>

            <section>
              <h3 className="text-sm font-semibold text-slate-900">Jury</h3>
              <div className="mt-3 grid gap-3 sm:grid-cols-3">
                {d.jury.map((j) => <Person key={j.professorId} title={ROLE_LABEL[j.role]} name={j.name} email={j.email} />)}
              </div>
            </section>

            <section>
              <h3 className="text-sm font-semibold text-slate-900">Le stage</h3>
              <dl className="mt-3 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
                <div><dt className="text-xs text-slate-400">Entreprise</dt><dd className="text-slate-800">{d.company ?? "-"}</dd></div>
                <div><dt className="text-xs text-slate-400">Encadrant en entreprise</dt><dd className="text-slate-800">{d.companySupervisor ?? "-"}</dd></div>
                <div><dt className="text-xs text-slate-400">Type</dt><dd className="text-slate-800">{d.kind ?? "-"}</dd></div>
                <div><dt className="text-xs text-slate-400">Période</dt><dd className="text-slate-800">{dateOnly(d.startDate)} - {dateOnly(d.endDate)}</dd></div>
              </dl>
            </section>

            <section className="grid gap-3 sm:grid-cols-2">
              {doc("report", "Rapport de stage (version finale)", d.hasReport, d.reportName, ".pdf")}
              {doc("record", "Procès-verbal : feuille de résultat", d.hasRecord, d.recordName, ".pdf,.png,.jpg,.jpeg")}
            </section>
            {preview && (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={preview} alt="Feuille de résultat" className="max-h-96 w-full rounded-xl border border-slate-200 object-contain" />
            )}
          </div>
        )}
      </div>
    </div>
  );
}