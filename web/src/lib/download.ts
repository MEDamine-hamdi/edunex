import { ApiError } from "./api";
import { getToken } from "./auth";

/** Authenticated download of any file endpoint (the token cannot travel in a plain link). */
export async function downloadFromUrl(url: string, fallbackName: string) {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  } catch {
    throw new ApiError(0, "Serveur injoignable.");
  }
  if (!res.ok) {
    throw new ApiError(res.status, res.status === 403 ? "Accès refusé." : "Téléchargement impossible.");
  }
  const blob = await res.blob();
  const match = /filename="?([^";]+)"?/i.exec(res.headers.get("Content-Disposition") ?? "");
  const objectUrl = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = objectUrl;
  a.download = match ? decodeURIComponent(match[1]) : fallbackName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(objectUrl);
}