export type Person = { id: number; name: string; email: string; number: string | null };
export type Role = "PRESIDENT" | "RAPPORTEUR" | "MEMBER";
export type Juror = { professorId: number; name: string; email: string; role: Role };
export type InternshipRow = {
  id: number;
  student: Person;
  supervisor: Person;
  company: string | null;
  companySupervisor: string | null;
  title: string;
  kind: string | null;
  startDate: string | null;
  endDate: string | null;
  defenseId: number | null;
  defenseStatus: string | null;
  defenseAt: string | null;
};
export type DefenseRow = {
  id: number;
  status: string;
  scheduledAt: string;
  room: string | null;
  student: Person;
  supervisor: Person;
  title: string;
  jury: Juror[];
  grade: number | null;
  mention: string | null;
};
export type DefenseDetail = {
  id: number;
  status: string;
  scheduledAt: string;
  room: string | null;
  grade: number | null;
  mention: string | null;
  decision: string | null;
  comments: string | null;
  internshipId: number;
  title: string;
  company: string | null;
  companySupervisor: string | null;
  kind: string | null;
  startDate: string | null;
  endDate: string | null;
  student: Person;
  supervisor: Person;
  jury: Juror[];
  hasReport: boolean;
  reportName: string | null;
  hasRecord: boolean;
  recordName: string | null;
};

export const ROLE_LABEL: Record<Role, string> = { PRESIDENT: "Président", RAPPORTEUR: "Rapporteur", MEMBER: "Membre" };

export const dateTime = (iso: string) =>
  new Date(iso).toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
export const dateOnly = (iso: string | null) =>
  iso ? new Date(iso).toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" }) : "-";

export function resultBadge(grade: number | null, status: string) {
  if (status !== "DONE" || grade == null) return { text: "À venir", cls: "bg-slate-200 text-slate-700" };
  return grade >= 10 ? { text: `${grade}/20`, cls: "bg-emerald-100 text-emerald-700" } : { text: `${grade}/20`, cls: "bg-red-100 text-red-700" };
}