import { getToken } from "./auth";

export async function fetchBlobUrl(path: string): Promise<string> {
  const token = getToken();
  const res = await fetch(path, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!res.ok) throw new Error("not found");
  return URL.createObjectURL(await res.blob());
}