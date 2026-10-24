"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { AuthLayout, authButton } from "@/components/AuthLayout";
import { InlineLoader } from "@/components/Loading";
import { publicPost } from "@/lib/auth";

export default function ConfirmEmailPage() {
  const [state, setState] = useState<"working" | "done" | "invalid">("working");
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setState("invalid");
      return;
    }
    window.history.replaceState(null, "", window.location.pathname);
    publicPost("/api/auth/confirm-email-change", { token })
      .then(() => setState("done"))
      .catch(() => setState("invalid"));
  }, []);

  return (
    <AuthLayout title="Adresse e-mail">
      {state === "working" && <div className="flex justify-center"><InlineLoader /></div>}
      {state === "done" && (
        <div>
          <p className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            Votre nouvelle adresse e-mail est confirmée. Reconnectez-vous avec cette adresse.
          </p>
          <Link href="/login" className={authButton}>Se connecter</Link>
        </div>
      )}
      {state === "invalid" && (
        <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Ce lien est incomplet, déjà utilisé ou n&apos;est plus valable. Refaites la demande depuis votre profil.
        </p>
      )}
    </AuthLayout>
  );
}