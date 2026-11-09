"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

type Row = { subjectId: number; name: string; eliminated: boolean };

/** Red banner for a student who is eliminated from one or more subjects. */
export function EliminationBanner() {
  const [names, setNames] = useState<string[]>([]);
  useEffect(() => {
    apiFetch<Row[]>("/api/student/elimination").then((rows) => setNames(rows.filter((r) => r.eliminated).map((r) => r.name))).catch(() => {});
  }, []);
  if (names.length === 0) return null;
  return (
    <p role="alert" className="mb-4 rounded-2xl border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800">
      Vous êtes éliminé(e) de : <span className="font-semibold">{names.join(", ")}</span> (trop d&apos;absences).{" "}
      <Link href="/student/elimination" className="font-semibold underline">Voir le détail</Link>
    </p>
  );
}