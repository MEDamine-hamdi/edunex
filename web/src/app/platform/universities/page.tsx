"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminAccountForm } from "@/components/AdminAccountForm";
import { PlatformShell } from "@/components/PlatformShell";
import { ErrorBox, PageHeader, inputCls, primaryBtn, smallBtn, tableWrap, theadCls } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type University = { id: number; name: string; code: string; city: string | null; status: string; agents: number; professors: number; students: number };
type Admin = { id: number; firstName: string; lastName: string; email: string; employeeNumber: string; service: string | null; status: string };

export default function UniversitiesPage() {
  const [rows, setRows] = useState<University[]>([]);
  const [form, setForm] = useState({ name: "", code: "", city: "" });
  const [showForm, setShowForm] = useState(false);
  const [openId, setOpenId] = useState<number | null>(null);
  const [admins, setAdmins] = useState<Admin[]>([]);
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown) => {
    if (e instanceof ApiError && e.status !== 401) setError(e.status === 409 ? `Conflit : ${e.message}` : e.message);
  };

  const load = useCallback(async () => {
    try {
      setRows(await apiFetch<University[]>("/api/platform/universities"));
      setError(null);
    } catch (e) {
      fail(e);
    }
  }, []);

  const loadAdmins = useCallback(async (id: number) => {
    try {
      setAdmins(await apiFetch<Admin[]>(`/api/platform/universities/${id}/admins`));
    } catch (e) {
      fail(e);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    try {
      await apiFetch("/api/platform/universities", { method: "POST", body: JSON.stringify({ name: form.name.trim(), code: form.code.trim(), city: form.city.trim() || null }) });
      setForm({ name: "", code: "", city: "" });
      setShowForm(false);
      await load();
    } catch (err) {
      fail(err);
    }
  }

  async function toggle(u: University) {
    const suspend = u.status === "ACTIVE";
    if (!window.confirm(suspend ? `Suspendre « ${u.name} » ? Ses utilisateurs ne pourront plus se connecter.` : `Réactiver « ${u.name} » ?`)) return;
    try {
      await apiFetch(`/api/platform/universities/${u.id}/status`, { method: "PUT", body: JSON.stringify({ status: suspend ? "SUSPENDED" : "ACTIVE" }) });
      await load();
    } catch (err) {
      fail(err);
    }
  }

  async function adminStatus(universityId: number, a: Admin) {
    try {
      await apiFetch(`/api/platform/universities/${universityId}/admins/${a.id}/status`, { method: "PUT", body: JSON.stringify({ status: a.status === "ACTIVE" ? "INACTIVE" : "ACTIVE" }) });
      await loadAdmins(universityId);
    } catch (err) {
      fail(err);
    }
  }

  return (
    <PlatformShell>
      <PageHeader
        title="Universités"
        subtitle="Créez les universités et leurs premiers administrateurs. Chaque université ne voit que ses propres données."
        action={<button onClick={() => setShowForm((v) => !v)} className={primaryBtn}>{showForm ? "Fermer" : "+ Nouvelle université"}</button>}
      />
      <ErrorBox message={error} />
      {showForm && (
        <form onSubmit={create} className="mt-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3">
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Nom<input required maxLength={200} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className={inputCls} /></label>
          <label className="block text-sm font-medium text-slate-700">Code (ex. UTUN)<input required maxLength={20} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} className={inputCls} /></label>
          <label className="block text-sm font-medium text-slate-700 sm:col-span-2">Ville<input maxLength={100} value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} className={inputCls} /></label>
          <div className="flex items-end"><button type="submit" className={primaryBtn}>Créer l&apos;université</button></div>
        </form>
      )}

      <div className={tableWrap}>
        <table className="w-full text-left text-sm">
          <thead className={theadCls}><tr><th className="px-5 py-3">Université</th><th className="px-5 py-3">Administrateurs</th><th className="px-5 py-3">Professeurs</th><th className="px-5 py-3">Étudiants</th><th className="px-5 py-3">Statut</th><th className="px-5 py-3 text-right">Actions</th></tr></thead>
          <tbody className="divide-y divide-slate-100">
            {rows.map((u) => (
              <tr key={u.id}>
                <td className="px-5 py-3 font-medium text-slate-900">{u.name}<span className="block text-xs font-normal text-slate-500">{u.code}{u.city ? ` · ${u.city}` : ""}</span></td>
                <td className="px-5 py-3 text-slate-700">{u.agents}</td>
                <td className="px-5 py-3 text-slate-700">{u.professors}</td>
                <td className="px-5 py-3 text-slate-700">{u.students}</td>
                <td className="px-5 py-3"><span className={`rounded-full px-3 py-1 text-xs font-semibold ${u.status === "ACTIVE" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"}`}>{u.status === "ACTIVE" ? "Active" : "Suspendue"}</span></td>
                <td className="px-5 py-3 text-right"><div className="flex justify-end gap-2">
                  <button onClick={() => { if (openId === u.id) setOpenId(null); else { setOpenId(u.id); loadAdmins(u.id); } }} className={smallBtn}>{openId === u.id ? "Fermer" : "Administrateurs"}</button>
                  <button onClick={() => toggle(u)} className={smallBtn}>{u.status === "ACTIVE" ? "Suspendre" : "Réactiver"}</button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {openId && (
        <section className="mt-6 space-y-5">
          <h2 className="text-lg font-semibold">Administrateurs de {rows.find((r) => r.id === openId)?.name}</h2>
          <div className={tableWrap}>
            <table className="w-full text-left text-sm">
              <thead className={theadCls}><tr><th className="px-5 py-3">Nom</th><th className="px-5 py-3">E-mail</th><th className="px-5 py-3">N° employé</th><th className="px-5 py-3">Statut</th><th className="px-5 py-3 text-right">Action</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {admins.length === 0 && <tr><td colSpan={5} className="px-5 py-6 text-center text-slate-500">Aucun administrateur : créez le premier ci-dessous.</td></tr>}
                {admins.map((a) => (
                  <tr key={a.id}>
                    <td className="px-5 py-3 font-medium text-slate-900">{a.firstName} {a.lastName}</td>
                    <td className="px-5 py-3 text-slate-700">{a.email}</td>
                    <td className="px-5 py-3 text-slate-700">{a.employeeNumber}</td>
                    <td className="px-5 py-3 text-slate-700">{a.status === "ACTIVE" ? "Actif" : "Inactif"}</td>
                    <td className="px-5 py-3 text-right"><button onClick={() => adminStatus(openId, a)} className={smallBtn}>{a.status === "ACTIVE" ? "Désactiver" : "Activer"}</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <AdminAccountForm endpoint={`/api/platform/universities/${openId}/admins`} onCreated={() => { loadAdmins(openId); load(); }} />
        </section>
      )}
    </PlatformShell>
  );
}