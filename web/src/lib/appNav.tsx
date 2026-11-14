import type { ReactNode } from "react";

/** The 4 tabs of the bottom bar, by route (the app's order). */
export const PRIMARY: Record<string, string[]> = {
  ROLE_STUDENT: ["dashboard", "courses", "timetable", "assistant"],
  ROLE_PROFESSOR: ["dashboard", "courses", "attendance", "timetable"],
  ROLE_ADMIN_AGENT: ["dashboard", "timetables", "announcements", "requests"],
  ROLE_PLATFORM_ADMIN: ["dashboard", "universities", "logs", "notifications"],
};

/** Order of the "Plus" page (the app's order). */
export const MORE_ORDER: Record<string, string[]> = {
  ROLE_STUDENT: ["subjects", "attendance", "elimination", "exams", "grades", "internship", "requests", "notifications", "profile"],
  ROLE_PROFESSOR: ["subjects", "classes", "internships", "exams", "delegated/grades", "delegated/timetable", "requests", "notifications", "profile"],
  ROLE_ADMIN_AGENT: ["professors", "students", "classes", "subjects", "enrollments", "internships", "grades", "exams", "agents", "elimination", "knowledge", "absences", "notifications", "profile"],
  ROLE_PLATFORM_ADMIN: ["roles", "permissions", "user-rights", "settings", "mail", "profile"],
};

const LABELS: Record<string, Record<string, string>> = {
  ROLE_STUDENT: { dashboard: "Accueil", courses: "Mes cours", timetable: "Emploi du temps", assistant: "Assistant IA", subjects: "Mes matières", attendance: "Absences", elimination: "Seuil d'absences", exams: "Mes examens", grades: "Notes", internship: "Mon stage", requests: "Demandes de documents", notifications: "Notifications", profile: "Mon profil" },
  ROLE_PROFESSOR: { dashboard: "Accueil", courses: "Mes cours", attendance: "Présences", timetable: "Emploi du temps", subjects: "Mes matières", classes: "Mes classes", internships: "Stages et soutenances", exams: "Surveillance des examens", "delegated/grades": "Saisie des notes", "delegated/timetable": "Gestion des emplois du temps", requests: "Demandes de documents", notifications: "Notifications", profile: "Mon profil" },
  ROLE_ADMIN_AGENT: { dashboard: "Accueil", timetables: "Emplois du temps", announcements: "Annonces", requests: "Demandes", professors: "Professeurs", students: "Étudiants", classes: "Classes", subjects: "Matières", enrollments: "Inscriptions", internships: "Stages et soutenances", grades: "Notes", exams: "Examens et surveillance", agents: "Administrateurs", elimination: "Éliminations", knowledge: "Base de connaissances", absences: "Absences des enseignants", notifications: "Notifications", profile: "Mon profil" },
  ROLE_PLATFORM_ADMIN: { dashboard: "Accueil", universities: "Universités", logs: "Journal", notifications: "Notifications", roles: "Rôles", permissions: "Permissions", "user-rights": "Droits par personne", settings: "Paramètres", mail: "E-mails", profile: "Mon profil" },
};

export const appLabel = (role: string, key: string, fallback: string) => LABELS[role]?.[key] ?? fallback;
export const bottomLabel = (label: string) => (label.startsWith("Emploi") ? "Planning" : label);
/** "/student/courses" -> "courses", "/professor/delegated/grades" -> "delegated/grades" */
export const keyOf = (href: string) => href.replace(/^\/[^/]+\/?/, "");

const COLORS: Record<string, string> = { ROLE_ADMIN_AGENT: "#2563EB", ROLE_PLATFORM_ADMIN: "#7C3AED", ROLE_PROFESSOR: "#059669", ROLE_STUDENT: "#0284C7" };
const NAMES: Record<string, string> = { ROLE_ADMIN_AGENT: "Administration", ROLE_PLATFORM_ADMIN: "Plateforme", ROLE_PROFESSOR: "Professeur", ROLE_STUDENT: "Étudiant" };
export const roleColor = (role: string) => COLORS[role] ?? "#0284C7";
export const roleLabel = (role: string) => NAMES[role] ?? role;

const ICONS: Record<string, ReactNode> = {
  dashboard: (<><rect width="7" height="9" x="3" y="3" rx="1" /><rect width="7" height="5" x="14" y="3" rx="1" /><rect width="7" height="9" x="14" y="12" rx="1" /><rect width="7" height="5" x="3" y="16" rx="1" /></>),
  book: (<><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" /><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" /></>),
  calendar: (<><rect width="18" height="18" x="3" y="4" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></>),
  sparkles: <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3z" />,
  grid: (<><rect width="7" height="7" x="3" y="3" rx="1" /><rect width="7" height="7" x="14" y="3" rx="1" /><rect width="7" height="7" x="14" y="14" rx="1" /><rect width="7" height="7" x="3" y="14" rx="1" /></>),
  calendarX: (<><rect width="18" height="18" x="3" y="4" rx="2" /><path d="M16 2v4M8 2v4M3 10h18M10 14l4 4M14 14l-4 4" /></>),
  alert: (<><path d="m21.7 18-8-14a2 2 0 0 0-3.4 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3z" /><path d="M12 9v4M12 17h.01" /></>),
  clipboard: (<><rect width="8" height="4" x="8" y="2" rx="1" /><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" /><path d="m9 14 2 2 4-4" /></>),
  star: <path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z" />,
  briefcase: (<><rect width="20" height="14" x="2" y="7" rx="2" /><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" /></>),
  file: (<><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z" /><path d="M14 2v4a2 2 0 0 0 2 2h4M10 9H8M16 13H8M16 17H8" /></>),
  bell: (<><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></>),
  user: (<><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" /></>),
  users: (<><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></>),
  cap: (<><path d="M22 10 12 5 2 10l10 5z" /><path d="M6 12v5c3 3 9 3 12 0v-5M22 10v6" /></>),
  userCheck: (<><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="m16 11 2 2 4-4" /></>),
  megaphone: (<><path d="m3 11 18-5v12L3 14v-3z" /><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6" /></>),
  bulb: (<><path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5" /><path d="M9 18h6M10 22h4" /></>),
  userX: (<><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="m17 8 5 5M22 8l-5 5" /></>),
  landmark: <path d="M3 22h18M6 18v-7M10 18v-7M14 18v-7M18 18v-7M12 2 3 7h18z" />,
  history: (<><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5M12 7v5l4 2" /></>),
  shield: <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />,
  key: (<><circle cx="7.5" cy="15.5" r="5.5" /><path d="m21 2-9.6 9.6M15.5 7.5l3 3L22 7l-3-3" /></>),
  userCog: (<><circle cx="9" cy="7" r="4" /><path d="M3 21v-2a4 4 0 0 1 4-4h4" /><circle cx="18" cy="18" r="3" /><path d="M18 14v1M18 21v1M21.5 16l-.9.5M15.4 19.5l-.9.5M21.5 20l-.9-.5M15.4 16.5l-.9-.5" /></>),
  sliders: <path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6" />,
  mail: (<><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></>),
  chat: <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22z" />,
  apps: (<><rect width="7" height="7" x="3" y="3" rx="1" /><rect width="7" height="7" x="14" y="3" rx="1" /><rect width="7" height="7" x="14" y="14" rx="1" /><rect width="7" height="7" x="3" y="14" rx="1" /></>),
  chevron: <path d="m9 18 6-6-6-6" />,
  back: <path d="m12 19-7-7 7-7M19 12H5" />,
  logout: (<><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5M21 12H9" /></>),
  globe: (<><circle cx="12" cy="12" r="10" /><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" /></>),
  sun: (<><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" /></>),
  mail2: <circle cx="12" cy="12" r="3" />,
};

const ICON_OF: Record<string, string> = {
  dashboard: "dashboard", courses: "book", timetable: "calendar", timetables: "calendar", assistant: "sparkles", subjects: "grid",
  elimination: "alert", exams: "clipboard", grades: "star", internship: "briefcase", internships: "briefcase", requests: "file",
  notifications: "bell", profile: "user", classes: "users", professors: "cap", students: "users", enrollments: "userCheck",
  announcements: "megaphone", knowledge: "bulb", absences: "userX", universities: "landmark", logs: "history", roles: "shield",
  permissions: "key", "user-rights": "userCog", settings: "sliders", mail: "mail", messages: "chat", agents: "shield",
  "delegated/grades": "star", "delegated/timetable": "calendar",
  apps: "apps", chevron: "chevron", back: "back", logout: "logout", globe: "globe", sun: "sun",
};

export function iconName(role: string, key: string): string {
  if (key === "attendance") return role === "ROLE_PROFESSOR" ? "userCheck" : "calendarX";
  return ICON_OF[key] ?? "mail2";
}

export function NavIcon({ name, role = "", className = "h-6 w-6" }: { name: string; role?: string; className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden="true">
      {ICONS[iconName(role, name)]}
    </svg>
  );
}