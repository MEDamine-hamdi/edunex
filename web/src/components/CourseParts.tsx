"use client";

import { useState } from "react";
import { ApiError, downloadDocument } from "@/lib/api";
import { formatDate, formatSize } from "@/lib/format";
import type { DocInfo } from "@/lib/types";
import { smallBtn } from "./ui";

export function DocumentRow({
  doc,
  onDelete,
  onError,
}: {
  doc: DocInfo;
  onDelete?: () => void;
  onError: (message: string) => void;
}) {
  const [busy, setBusy] = useState(false);

  async function download() {
    setBusy(true);
    try {
      await downloadDocument(doc.id, doc.title);
    } catch (e) {
      onError(e instanceof ApiError ? e.message : "Téléchargement impossible.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 px-4 py-3">
      <div className="flex min-w-0 items-center gap-3">
        <span className="rounded-md bg-blue-100 px-2 py-1 text-[11px] font-bold text-blue-700">
          {doc.type ?? "FILE"}
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-slate-900">{doc.title}</p>
          <p className="text-xs text-slate-500">
            {formatSize(doc.size)} · {formatDate(doc.uploadedAt)}
          </p>
        </div>
      </div>
      <div className="flex gap-2">
        <button onClick={download} disabled={busy} className={smallBtn}>
          {busy ? "..." : "Télécharger"}
        </button>
        {onDelete && (
          <button onClick={onDelete} className={`${smallBtn} text-red-600`}>
            Supprimer
          </button>
        )}
      </div>
    </li>
  );
}