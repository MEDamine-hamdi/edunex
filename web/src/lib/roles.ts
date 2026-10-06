export function homeFor(role: string | null): string {
  switch (role) {
    case "ROLE_PLATFORM_ADMIN":
      return "/platform/agents";
    case "ROLE_ADMIN_AGENT":
      return "/admin/professors";
    case "ROLE_PROFESSOR":
      return "/professor";
    case "ROLE_STUDENT":
      return "/student";
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