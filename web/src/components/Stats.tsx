import { ReactNode } from "react";

const TONES = {
  slate: "text-slate-900",
  blue: "text-blue-700",
  green: "text-emerald-700",
  red: "text-red-600",
  orange: "text-orange-600",
};

export function StatCard({
  label,
  value,
  hint,
  tone = "slate",
}: {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: keyof typeof TONES;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className={`mt-2 text-3xl font-extrabold ${TONES[tone]}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

export function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold">{title}</h2>
      {children}
    </section>
  );
}

export function BarList({
  items,
  max,
  suffix = "",
  color = "bg-blue-500",
}: {
  items: { label: string; value: number }[];
  max?: number;
  suffix?: string;
  color?: string;
}) {
  if (items.length === 0) return <p className="text-sm text-slate-500">Pas encore de données.</p>;
  const top = max ?? Math.max(1, ...items.map((i) => i.value));
  return (
    <ul className="space-y-3">
      {items.map((i) => (
        <li key={i.label}>
          <div className="flex justify-between text-sm">
            <span className="text-slate-700">{i.label}</span>
            <span className="font-semibold text-slate-900">{i.value}{suffix}</span>
          </div>
          <div className="mt-1 h-2 rounded-full bg-slate-100">
            <div className={`h-2 rounded-full ${color}`} style={{ width: `${Math.min(100, (i.value / top) * 100)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}