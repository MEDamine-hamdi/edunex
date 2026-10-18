"use client";

import { ChangeEvent, FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { fetchBlobUrl } from "@/lib/blob";
import { roleLabels } from "@/lib/roles";
import { ErrorBox, PageHeader, TextField, primaryBtn, smallBtn } from "./ui";

type Profile = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  role: string | null;
  status: string;
  address: string | null;
  dateOfBirth: string | null;
  hasPhoto: boolean;
  details: { label: string; value: string }[];
};

function photoError(e: ApiError) {
  if (e.status === 413 || e.message.includes("too large")) return "Photo trop volumineuse (2 Mo maximum).";
  if (e.message.includes("PNG")) return "Format non supporté (PNG, JPG ou WEBP).";
  return e.message;
}

function passwordError(e: ApiError) {
  if (e.message.includes("incorrect")) return "Mot de passe actuel incorrect.";
  if (e.message.includes("different")) return "Le nouveau mot de passe doit être différent de l'actuel.";
  if (e.message.includes("newPassword")) return "Le nouveau mot de passe doit contenir 8 caractères minimum.";
  return e.message;
}

const todayStr = () => new Date().toISOString().slice(0, 10);

export function ProfileEditor() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [form, setForm] = useState({ phone: "", address: "", dateOfBirth: "" });
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [photoVersion, setPhotoVersion] = useState(0);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const [pwSaving, setPwSaving] = useState(false);
  const [pwInfo, setPwInfo] = useState<string | null>(null);
  const [pwError, setPwError] = useState<string | null>(null);

  const apply = useCallback((p: Profile) => {
    setProfile(p);
    setForm({ phone: p.phone ?? "", address: p.address ?? "", dateOfBirth: p.dateOfBirth ?? "" });
  }, []);

  useEffect(() => {
    apiFetch<Profile>("/api/profile")
      .then(apply)
      .catch((e) => {
        if (e instanceof ApiError && e.status !== 401) setError(e.message);
      });
  }, [apply]);

  const hasPhoto = profile?.hasPhoto ?? false;
  useEffect(() => {
    if (!hasPhoto) {
      setPhotoUrl(null);
      return;
    }
    let url: string | null = null;
    let cancelled = false;
    fetchBlobUrl("/api/profile/photo")
      .then((u) => {
        if (cancelled) URL.revokeObjectURL(u);
        else {
          url = u;
          setPhotoUrl(u);
        }
      })
      .catch(() => setPhotoUrl(null));
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [hasPhoto, photoVersion]);

  async function save(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setInfo(null);
    setError(null);
    try {
      apply(
        await apiFetch<Profile>("/api/profile", {
          method: "PUT",
          body: JSON.stringify({
            phone: form.phone.trim() || null,
            address: form.address.trim() || null,
            dateOfBirth: form.dateOfBirth || null,
          }),
        })
      );
      setInfo("Profil enregistré.");
    } catch (err) {
      if (err instanceof ApiError) setError(err.status === 400 ? "Vérifiez les champs (la date de naissance doit être passée)." : err.message);
    } finally {
      setSaving(false);
    }
  }

  async function upload(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      apply(await apiFetch<Profile>("/api/profile/photo", { method: "POST", body: fd }));
      setPhotoVersion((v) => v + 1);
    } catch (err) {
      if (err instanceof ApiError) setError(photoError(err));
    } finally {
      setUploading(false);
    }
  }

  async function removePhoto() {
    setError(null);
    try {
      apply(await apiFetch<Profile>("/api/profile/photo", { method: "DELETE" }));
    } catch (err) {
      if (err instanceof ApiError) setError(err.message);
    }
  }

  async function changePassword(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPwInfo(null);
    setPwError(null);
    if (pw.next !== pw.confirm) {
      setPwError("La confirmation ne correspond pas au nouveau mot de passe.");
      return;
    }
    setPwSaving(true);
    try {
      await apiFetch<void>("/api/profile/password", {
        method: "PUT",
        body: JSON.stringify({ currentPassword: pw.current, newPassword: pw.next }),
      });
      setPw({ current: "", next: "", confirm: "" });
      setPwInfo("Mot de passe modifié.");
    } catch (err) {
      if (err instanceof ApiError) setPwError(passwordError(err));
    } finally {
      setPwSaving(false);
    }
  }

  if (!profile) return <p className="text-slate-500">{error ?? "Chargement..."}</p>;

  const initials = `${profile.firstName[0] ?? ""}${profile.lastName[0] ?? ""}`.toUpperCase();
  const card = "rounded-2xl border border-slate-200 bg-white p-6 shadow-sm";

  return (
    <>
      <PageHeader title="Mon profil" subtitle={roleLabels[profile.role ?? ""] ?? ""} />
      <ErrorBox message={error} />
      {info && <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{info}</p>}

      <div className="mt-6 grid max-w-5xl gap-6 lg:grid-cols-[18rem_1fr]">
        <section className={`${card} text-center`}>
          {photoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={photoUrl} alt="Photo de profil" className="mx-auto h-32 w-32 rounded-full object-cover" />
          ) : (
            <div className="mx-auto flex h-32 w-32 items-center justify-center rounded-full bg-blue-100 text-4xl font-bold text-blue-700">
              {initials}
            </div>
          )}
          <h2 className="mt-4 text-lg font-semibold">{profile.firstName} {profile.lastName}</h2>
          <p className="text-sm text-slate-500">{profile.email}</p>
          <div className="mt-4 flex justify-center gap-2">
            <label className={`cursor-pointer ${smallBtn}`}>
              {uploading ? "Envoi..." : hasPhoto ? "Changer" : "Ajouter une photo"}
              <input type="file" hidden accept="image/png,image/jpeg,image/webp" onChange={upload} disabled={uploading} />
            </label>
            {hasPhoto && <button onClick={removePhoto} className={`${smallBtn} text-red-600`}>Retirer</button>}
          </div>
          <p className="mt-2 text-xs text-slate-400">PNG, JPG ou WEBP, 2 Mo maximum.</p>
        </section>

        <div className="space-y-6">
          <form onSubmit={save} className={card}>
            <h2 className="text-lg font-semibold">Informations personnelles</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <TextField label="Téléphone" maxLength={30} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              <TextField label="Date de naissance" type="date" max={todayStr()} value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
              <div className="sm:col-span-2">
                <TextField label="Adresse" maxLength={255} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
            </div>
            <button type="submit" disabled={saving} className={`mt-6 ${primaryBtn}`}>
              {saving ? "Enregistrement..." : "Enregistrer"}
            </button>
          </form>

          {profile.details.length > 0 && (
            <section className={card}>
              <h2 className="text-lg font-semibold">Informations du compte</h2>
              <p className="mt-1 text-sm text-slate-500">Ces informations sont gérées par l&apos;administration.</p>
              <dl className="mt-4 divide-y divide-slate-100 text-sm">
                {profile.details.map((d) => (
                  <div key={d.label} className="flex justify-between gap-4 py-3">
                    <dt className="text-slate-500">{d.label}</dt>
                    <dd className="font-medium text-slate-900">{d.value}</dd>
                  </div>
                ))}
              </dl>
            </section>
          )}

          <form onSubmit={changePassword} className={card}>
            <h2 className="text-lg font-semibold">Changer le mot de passe</h2>
            {pwError && <p role="alert" className="mt-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{pwError}</p>}
            {pwInfo && <p className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{pwInfo}</p>}
            <div className="mt-4 grid gap-4 sm:grid-cols-3">
              <TextField label="Mot de passe actuel" type="password" autoComplete="current-password" value={pw.current} onChange={(e) => setPw({ ...pw, current: e.target.value })} required />
              <TextField label="Nouveau (8 car. min.)" type="password" autoComplete="new-password" minLength={8} value={pw.next} onChange={(e) => setPw({ ...pw, next: e.target.value })} required />
              <TextField label="Confirmation" type="password" autoComplete="new-password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} required />
            </div>
            <button type="submit" disabled={pwSaving} className={`mt-6 ${primaryBtn}`}>
              {pwSaving ? "Modification..." : "Modifier le mot de passe"}
            </button>
          </form>
        </div>
      </div>
    </>
  );
}