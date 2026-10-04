export function Logo({ size = "md" }: { size?: "md" | "lg" }) {
  const icon = size === "lg" ? "h-12 w-12" : "h-10 w-10";
  const text = size === "lg" ? "text-4xl" : "text-3xl";
  return (
    <div className="flex items-center gap-3">
      <svg viewBox="0 0 48 48" className={icon} fill="none" aria-hidden="true">
        <defs>
          <linearGradient id="edunex-logo" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop stopColor="#7c3aed" />
            <stop offset="1" stopColor="#2563eb" />
          </linearGradient>
        </defs>
        <path d="M24 6 3 16l21 10 21-10L24 6Z" fill="url(#edunex-logo)" />
        <path d="M11 22v9c0 3.5 5.8 7 13 7s13-3.5 13-7v-9l-13 6-13-6Z" fill="url(#edunex-logo)" opacity=".85" />
        <path d="M43 17v12" stroke="url(#edunex-logo)" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span className={`${text} font-extrabold tracking-tight text-slate-900`}>EduNex</span>
    </div>
  );
}