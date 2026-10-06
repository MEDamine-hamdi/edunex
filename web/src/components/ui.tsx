"use client";

import { InputHTMLAttributes, ReactNode, useState } from "react";

export const inputCls =
  "mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[15px] text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100";
export const primaryBtn =
  "rounded-xl bg-blue-600 px-5 py-3 text-sm font-medium text-white shadow-lg shadow-blue-600/25 hover:bg-blue-700 disabled:opacity-60";
export const smallBtn =
  "rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50";
export const tableWrap = "mt-6 overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm";
export const theadCls =
  "border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500";

export function ErrorBox({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </p>
  );
}

export function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 className="text-2xl font-extrabold tracking-tight">{title}</h1>
        <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
      </div>
      {action}
    </div>
  );
}

export function TextField({ label, ...props }: { label: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <input {...props} className={inputCls} />
    </label>
  );
}

export function IdPicker({
  options,
  selected,
  onSave,
  saving,
  emptyText,
}: {
  options: { id: number; label: string }[];
  selected: number[];
  onSave: (ids: number[]) => void;
  saving?: boolean;
  emptyText: string;
}) {
  const [ids, setIds] = useState<number[]>(selected);
  const toggle = (id: number) =>
    setIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  if (options.length === 0) return <p className="text-sm text-slate-500">{emptyText}</p>;

  return (
    <div>
      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {options.map((o) => (
          <label
            key={o.id}
            className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm"
          >
            <input
              type="checkbox"
              checked={ids.includes(o.id)}
              onChange={() => toggle(o.id)}
              className="h-4 w-4 accent-blue-600"
            />
            {o.label}
          </label>
        ))}
      </div>
      <button onClick={() => onSave(ids)} disabled={saving} className={`mt-4 ${primaryBtn}`}>
        {saving ? "Enregistrement..." : "Enregistrer"}
      </button>
    </div>
  );
}