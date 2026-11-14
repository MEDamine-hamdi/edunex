/** The logo of the mobile app: gradient square with the school icon, then "EduNex". */
export function AppLogo({ size = 36, withText = true }: { size?: number; withText?: boolean }) {
  return (
    <span className="inline-flex items-center">
      <span
        className="flex items-center justify-center"
        style={{ width: size, height: size, borderRadius: size * 0.28, background: "linear-gradient(135deg,#7C3AED,#2563EB)" }}
      >
        <svg viewBox="0 0 24 24" width={size * 0.62} height={size * 0.62} fill="#fff" aria-hidden="true">
          <path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3 1 9l11 6 9-4.91V17h2V9L12 3z" />
        </svg>
      </span>
      {withText && (
        <span className="font-black text-slate-900" style={{ marginInlineStart: size * 0.3, fontSize: size * 0.58, letterSpacing: -0.5 }}>
          EduNex
        </span>
      )}
    </span>
  );
}