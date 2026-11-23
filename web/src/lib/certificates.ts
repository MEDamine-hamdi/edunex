export type CertStatus = "PENDING" | "APPROVED" | "REJECTED" | "REVOKED" | "WITHDRAWN";
export type CertRow = {
  id: number; studentId: number; studentName: string; studentNumber: string; source: "APP" | "IN_PERSON"; status: CertStatus;
  startDate: string; endDate: string; note: string | null; hasFile: boolean; fileName: string | null; createdAt: string;
  createdBy: string | null; decidedAt: string | null; decidedBy: string | null; decisionNote: string | null;
};
export type AbsenceItem = { attendanceId: number; date: string; subjectName: string; status: string; justification: string | null };
export type ExamItem = { examId: number; subjectName: string; examType: string; date: string; startTime: string; room: string | null };
export type Coverage = { absences: AbsenceItem[]; exams: ExamItem[] };
export type RetakeItem = {
  id: number; examId: number; subjectName: string; examType: string; originalDate: string; studentId: number; studentName: string;
  studentNumber: string; status: "TO_SCHEDULE" | "SCHEDULED" | "DONE" | "CANCELLED"; date: string | null; startTime: string | null; endTime: string | null; room: string | null;
};
export type Detail = { row: CertRow; absences: AbsenceItem[]; exams: ExamItem[]; retakes: RetakeItem[] };

export const STATUS: Record<CertStatus, { label: string; cls: string }> = {
  PENDING: { label: "En attente de vérification", cls: "bg-amber-100 text-amber-800" },
  APPROVED: { label: "Validé", cls: "bg-emerald-100 text-emerald-700" },
  REJECTED: { label: "Refusé", cls: "bg-red-100 text-red-700" },
  REVOKED: { label: "Validation annulée", cls: "bg-slate-200 text-slate-700" },
  WITHDRAWN: { label: "Retiré", cls: "bg-slate-200 text-slate-600" },
};
export const RETAKE: Record<RetakeItem["status"], { label: string; cls: string }> = {
  TO_SCHEDULE: { label: "À planifier", cls: "bg-amber-100 text-amber-800" },
  SCHEDULED: { label: "Planifié", cls: "bg-blue-100 text-blue-700" },
  DONE: { label: "Passé", cls: "bg-emerald-100 text-emerald-700" },
  CANCELLED: { label: "Annulé", cls: "bg-slate-200 text-slate-600" },
};

export const shortDate = (iso: string) => new Date(iso + "T12:00:00").toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
export const period = (a: string, b: string) => (a === b ? shortDate(a) : `${shortDate(a)} - ${shortDate(b)}`);
export const hour = (t: string | null) => (t ? t.slice(0, 5) : "");
export const stamp = (iso: string) => new Date(iso).toLocaleString(undefined, { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });

export function explain(message: string, status: number): string {
  if (status === 429) return "Trop de demandes : patientez un instant.";
  if (message.includes("deadline")) return "Le délai pour envoyer ce certificat par l'application est dépassé : remettez-le à l'administration.";
  if (message.includes("already covers")) return "Un certificat couvre déjà ces dates.";
  if (message.includes("3 certificates")) return "Vous avez déjà 3 certificats en attente de vérification.";
  if (message.includes("must be a PDF")) return "Le certificat doit être un PDF, PNG ou JPG de 5 Mo maximum.";
  if (message.includes("file is required")) return "Joignez le fichier du certificat.";
  if (message.includes("No absence or exam")) return "Aucune absence ni aucun examen dans cette période.";
  if (message.includes("at most 60 days")) return "Un certificat couvre 60 jours au maximum.";
  if (message.includes("too old")) return "Cette période est trop ancienne.";
  if (message.includes("Invalid period")) return "Période invalide.";
  if (message.includes("already used") || message.includes("room")) return "Salle déjà utilisée à ce moment.";
  if (message.includes("has an exam")) return "Un étudiant a déjà un examen à ce moment.";
  if (message.includes("make-up exam at this time")) return "Un étudiant a déjà un rattrapage à ce moment.";
  if (status === 403) return "Action non autorisée.";
  return message;
}