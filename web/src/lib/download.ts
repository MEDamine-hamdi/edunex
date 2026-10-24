import { ApiError } from "./api";

/** Download of any protected file endpoint (the session cookie is sent by the browser). */
export async function downloadFromUrl(url: string, fallbackName: string) {
  let res: Response;
  try {
    res = await fetch(url);
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