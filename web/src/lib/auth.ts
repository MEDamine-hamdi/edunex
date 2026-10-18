export type LoginResponse = {
  token: string;
  type: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string | null;
};

const TOKEN_KEY = "edunex_token";

export async function login(email: string, password: string): Promise<LoginResponse> {
  let res: Response;
  try {
    res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
  } catch {
    throw new Error("Serveur injoignable. Vérifiez que le backend est démarré.");
  }

  if (!res.ok) {
    let message = "Une erreur est survenue. Réessayez.";
    try {
      const data = await res.json();
      if (res.status === 429) message = "Trop de tentatives échouées. Réessayez dans quelques minutes.";
      else if (res.status === 401) message = "Email ou mot de passe incorrect.";
      else if (res.status === 403) message = "Ce compte n'est pas actif.";
      else if (data?.fields) message = "Veuillez saisir un email valide et un mot de passe.";
      else if (data?.error) message = data.error;
    } catch {
      if (res.status === 429) message = "Trop de tentatives échouées. Réessayez dans quelques minutes.";
      else if (res.status === 401) message = "Email ou mot de passe incorrect.";
    }
    throw new Error(message);
  }
  return res.json();
}

export function saveSession(data: LoginResponse, remember: boolean) {
  clearSession();
  (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, data.token);
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
}