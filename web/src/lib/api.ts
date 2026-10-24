export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const PUBLIC_PAGES = ["/login", "/forgot-password", "/reset-password", "/confirm-email"];

function handleUnauthorized() {
  if (typeof window !== "undefined" && !PUBLIC_PAGES.includes(window.location.pathname)) {
    window.location.replace("/login");
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const isForm = typeof FormData !== "undefined" && init.body instanceof FormData;
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      headers: {
        ...(isForm ? {} : { "Content-Type": "application/json" }),
        "X-Requested-With": "XMLHttpRequest",
        ...(init.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError(0, "Serveur injoignable. Vérifiez que le backend est démarré.");
  }

  if (res.status === 401) {
    handleUnauthorized();
    throw new ApiError(401, "Session expirée.");
  }

  if (!res.ok) {
    let message = "Une erreur est survenue.";
    try {
      const data = await res.json();
      if (res.status === 403) message = "Accès refusé.";
      else if (data?.fields)
        message = Object.entries(data.fields).map(([k, v]) => `${k} : ${v}`).join(", ");
      else if (data?.error) message = data.error;
    } catch {
      /* no JSON body */
    }
    throw new ApiError(res.status, message);
  }

  return res.status === 204 ? (undefined as T) : res.json();
}

export async function downloadDocument(id: number, fallbackName: string) {
  let res: Response;
  try {
    res = await fetch(`/api/documents/${id}/download`);
  } catch {
    throw new ApiError(0, "Serveur injoignable.");
  }
  if (res.status === 401) {
    handleUnauthorized();
    throw new ApiError(401, "Session expirée.");
  }
  if (!res.ok) {
    throw new ApiError(res.status, res.status === 403 ? "Accès refusé." : "Téléchargement impossible.");
  }

  const blob = await res.blob();
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1]) : fallbackName;

  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}