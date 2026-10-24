/** Fetches a protected file (the session cookie is sent by the browser) and returns a temporary URL for it. */
export async function fetchBlobUrl(path: string): Promise<string> {
  const res = await fetch(path);
  if (!res.ok) throw new Error("not found");
  return URL.createObjectURL(await res.blob());
}