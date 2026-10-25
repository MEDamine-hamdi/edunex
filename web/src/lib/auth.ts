export type LoginResponse = {
  token: string | null;
  type: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string | null;
};

// "X-Requested-With" is required by the server for requests that change data (protection against forged requests)
const JSON_HEADERS = { "Content-Type": "application/json", "X-Requested-With": "XMLHttpRequest" };

async function messageOf(res: Response, fallback: string): Promise<string> {
  try {
    const data = await res.json();
    if (data?.fields) return Object.entries(data.fields).map(([k, v]) => `${k} : ${v}`).join(", ");
    if (data?.error) return data.error;
  } catch {
    /* no JSON body */
  }
  return fallback;
}

export async function login(email: string, password: string, remember: boolean): Promise<LoginResponse> {
  let res: Response;
  try {
    res = await fetch("/api/auth/login", {
      method: "POST",
      headers: JSON_HEADERS,
      body: JSON.stringify({ email, password, remember }),
    });
  } catch {
    throw new Error("Serveur injoignable. Vérifiez que le backend est démarré.");
  }
  if (!res.ok) {
    let message = `Une erreur est survenue (code ${res.status}). Réessayez.`;
    if (res.status === 401) message = "Email ou mot de passe incorrect.";
    else if (res.status === 403) message = "Ce compte n'est pas actif.";
    else if (res.status === 429) message = "Trop de tentatives échouées. Réessayez dans quelques minutes.";
    else if (res.status === 400) message = "Veuillez saisir un email valide et un mot de passe.";
    else if (res.status === 404) message = "Serveur introuvable : vérifiez l'adresse du backend (BACKEND_URL).";
    else if (res.status >= 500) message = `Le serveur ne répond pas correctement (code ${res.status}). Réessayez dans un instant.`;
    throw new Error(message);
  }
  return res.json();
}

/** Ends the session: the server deletes the cookie. */
export async function logout(): Promise<void> {
  try {
    await fetch("/api/auth/logout", { method: "POST", headers: JSON_HEADERS });
  } catch {
    /* the cookie expires by itself */
  }
}

/** True when the browser has a valid session. Never redirects (used by the login page). */
export async function hasSession(): Promise<boolean> {
  try {
    const res = await fetch("/api/users/me", { headers: { "X-Requested-With": "XMLHttpRequest" } });
    return res.ok;
  } catch {
    return false;
  }
}

/** POST for the pages that do not need a session (forgotten password, links received by e-mail). */
export async function publicPost<T = unknown>(path: string, body: unknown): Promise<T> {
  let res: Response;
  try {
    res = await fetch(path, { method: "POST", headers: JSON_HEADERS, body: JSON.stringify(body) });
  } catch {
    throw new Error("Serveur injoignable. Vérifiez votre connexion.");
  }
  if (!res.ok) {
    const message =
      res.status === 429
        ? "Trop de demandes. Réessayez dans quelques minutes."
        : await messageOf(res, "Une erreur est survenue. Réessayez.");
    throw Object.assign(new Error(message), { status: res.status });
  }
  return res.status === 204 ? (undefined as T) : res.json();
}