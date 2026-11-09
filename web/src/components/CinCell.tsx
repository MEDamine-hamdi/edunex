"use client";

import { useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";

/** Shows the CIN; lets the administration fill it in for accounts created before the field existed. */
export function CinCell({ userId, value }: { userId: number; value: string | null }) {
  const [cin, setCin] = useState(value);

  async function edit() {
    const answer = window.prompt("CIN (8 chiffres)", cin ?? "");
    if (answer === null) return;
    const next = answer.trim();
    if (!/^\d{8}$/.test(next)) {
      window.alert("Le CIN doit contenir exactement 8 chiffres.");
      return;
    }
    try {
      await apiFetch(`/api/admin/users/${userId}/cin`, { method: "PUT", body: JSON.stringify({ cin: next }) });
      setCin(next);
    } catch (e) {
      window.alert(e instanceof ApiError && e.status === 409 ? "Ce CIN est déjà utilisé par un autre compte." : "Enregistrement impossible.");
    }
  }

  return (
    <span className="inline-flex items-center gap-2">
      <span className={cin ? "font-mono" : "text-slate-400"}>{cin ?? "-"}</span>
      <button onClick={edit} className="rounded-lg border border-slate-200 px-2 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50">Modifier</button>
    </span>
  );
}