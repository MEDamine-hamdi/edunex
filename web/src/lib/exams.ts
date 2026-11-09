export type ClassItem = { id: number; name: string };
export type SupervisorItem = { professorId: number; name: string; email: string; kind: string; mode: string; assignedBy: string | null };
export type ExamItem = {
  id: number;
  examType: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  classes: ClassItem[];
  date: string;
  startTime: string;
  endTime: string;
  room: string | null;
  needed: number;
  status: string;
  cancelReason: string | null;
  supervisors: SupervisorItem[];
  students: number;
};
export type EventRow = { id: number; examId: number | null; exam: string | null; professorId: number; professor: string; action: string; actor: string | null; reason: string | null; at: string };

export const ACTION_LABEL: Record<string, string> = {
  VOLUNTEERED: "S'est porté volontaire",
  ASSIGNED: "Affecté par l'administration",
  WITHDRAWN: "S'est désisté",
  REMOVED: "Retiré par l'administration",
  EXAM_CANCELLED: "Examen annulé",
};

export const fullDate = (iso: string) => new Date(iso + "T12:00:00").toLocaleDateString(undefined, { weekday: "long", day: "2-digit", month: "long", year: "numeric" });
export const shortDate = (iso: string) => new Date(iso + "T12:00:00").toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
export const hour = (t: string) => t.slice(0, 5);
export const stamp = (iso: string) => new Date(iso).toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });