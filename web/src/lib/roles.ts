export function homeFor(role: string | null): string {
  switch (role) {
    case "ROLE_PLATFORM_ADMIN":
      return "/platform/dashboard";
    case "ROLE_ADMIN_AGENT":
      return "/admin/dashboard";
    case "ROLE_PROFESSOR":
      return "/professor";
    case "ROLE_STUDENT":
      return "/student";
    default:
      return "/login";
  }
}

export function notificationsFor(role: string | null): string {
  switch (role) {
    case "ROLE_PLATFORM_ADMIN":
      return "/platform/notifications";
    case "ROLE_ADMIN_AGENT":
      return "/admin/notifications";
    case "ROLE_PROFESSOR":
      return "/professor/notifications";
    case "ROLE_STUDENT":
      return "/student/notifications";
    default:
      return "/login";
  }
}

export function profileFor(role: string | null): string {
  switch (role) {
    case "ROLE_PLATFORM_ADMIN":
      return "/platform/profile";
    case "ROLE_ADMIN_AGENT":
      return "/admin/profile";
    case "ROLE_PROFESSOR":
      return "/professor/profile";
    case "ROLE_STUDENT":
      return "/student/profile";
    default:
      return "/login";
  }
}

export const roleLabels: Record<string, string> = {
  ROLE_PLATFORM_ADMIN: "Administrateur de la plateforme",
  ROLE_ADMIN_AGENT: "Agent d'administration universitaire",
  ROLE_PROFESSOR: "Professeur",
  ROLE_STUDENT: "Étudiant",
};