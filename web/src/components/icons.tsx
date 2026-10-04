import type { SVGProps } from "react";

type P = SVGProps<SVGSVGElement>;

const stroke = (p: P) => ({
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
  ...p,
});

export const MailIcon = (p: P) => (
  <svg {...stroke(p)}><rect x="3" y="5" width="18" height="14" rx="2.5" /><path d="m3.5 7.5 8.5 6 8.5-6" /></svg>
);
export const LockIcon = (p: P) => (
  <svg {...stroke(p)}><rect x="4" y="10.5" width="16" height="10" rx="2.5" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" /></svg>
);
export const EyeIcon = (p: P) => (
  <svg {...stroke(p)}><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>
);
export const EyeOffIcon = (p: P) => (
  <svg {...stroke(p)}><path d="M3 3l18 18" /><path d="M10.6 5.1A9.7 9.7 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-3.2 4.1M6.6 6.6A16.6 16.6 0 0 0 2 12s3.6 7 10 7a9.6 9.6 0 0 0 4.4-1" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></svg>
);
export const ArrowRightIcon = (p: P) => (
  <svg {...stroke(p)}><path d="M5 12h14M13 6l6 6-6 6" /></svg>
);
export const BookIcon = (p: P) => (
  <svg {...stroke(p)}><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5v-16Z" /><path d="M4 21.5A2.5 2.5 0 0 1 6.5 19H20" /></svg>
);
export const CalendarIcon = (p: P) => (
  <svg {...stroke(p)}><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4M8 14h.01M12 14h.01M16 14h.01M8 17.5h.01M12 17.5h.01" /></svg>
);
export const ChartIcon = (p: P) => (
  <svg {...stroke(p)} strokeWidth={2.6}><path d="M5 20v-8M12 20V5M19 20v-11" /></svg>
);
export const BotIcon = (p: P) => (
  <svg {...stroke(p)}><rect x="4" y="8" width="16" height="11" rx="3.5" /><path d="M12 8V4.5M2.5 13v3M21.5 13v3M9 13.5h.01M15 13.5h.01" /></svg>
);
export const GlobeIcon = (p: P) => (
  <svg {...stroke(p)}><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a14 14 0 0 1 0 18 14 14 0 0 1 0-18Z" /></svg>
);
export const ChevronDownIcon = (p: P) => (
  <svg {...stroke(p)}><path d="m6 9 6 6 6-6" /></svg>
);
export const GoogleIcon = (p: P) => (
  <svg viewBox="0 0 48 48" aria-hidden="true" {...p}>
    <path fill="#FFC107" d="M43.6 20.1H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 8 3l5.7-5.7C34 6.1 29.3 4 24 4 13 4 4 13 4 24s9 20 20 20 20-9 20-20c0-1.3-.1-2.3-.4-3.9z" />
    <path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 8 3l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
    <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z" />
    <path fill="#1976D2" d="M43.6 20.1H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.3-.4-3.9z" />
  </svg>
);
export const MicrosoftIcon = (p: P) => (
  <svg viewBox="0 0 24 24" aria-hidden="true" {...p}>
    <rect x="2" y="2" width="9.5" height="9.5" fill="#F25022" />
    <rect x="12.5" y="2" width="9.5" height="9.5" fill="#7FBA00" />
    <rect x="2" y="12.5" width="9.5" height="9.5" fill="#00A4EF" />
    <rect x="12.5" y="12.5" width="9.5" height="9.5" fill="#FFB900" />
  </svg>
);