"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

export function EmailPreferenceCard() {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    apiFetch<{ enabled: boolean }>("/api/profile/email-notifications")
      .then((r) => setEnabled(r.enabled))
      .catch(() => setEnabled(true));
  }, []);

  async function change(value: boolean) {
    setEnabled(value);
    setError(false);
    try {
      await apiFetch("/api/profile/email-notifications", { method: "PUT", body: JSON.stringify({ enabled: value }) });
    } catch {
      setEnabled(!value);
      setError(true);
    }
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Notifications par e-mail</h2>
      <p className="mt-1 text-sm text-slate-500">
        Annonces, notes, séances annulées, demandes de documents : recevez-les aussi dans votre boîte mail.
      </p>
      <label className="mt-4 flex cursor-pointer items-center gap-3 text-sm text-slate-700">
        <input type="checkbox" className="h-5 w-5 accent-blue-600" checked={enabled ?? true} disabled={enabled === null}
          onChange={(e) => change(e.target.checked)} />
        Recevoir mes notifications par e-mail
      </label>
      {error && <p className="mt-3 text-sm text-red-600">Impossible d&apos;enregistrer ce choix.</p>}
    </section>
  );
}