import { clearSession, getToken } from "./auth";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(path, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(init.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError(0, "Serveur injoignable. Vérifiez que le backend est démarré.");
  }

  if (res.status === 401) {
    clearSession();
    if (typeof window !== "undefined") window.location.href = "/login";
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