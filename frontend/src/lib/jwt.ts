/** Decode JWT payload without verifying signature — used only for client-side expiry checks. */
export function getAccessTokenExpiresAt(token: string): number | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4);
    const json = JSON.parse(atob(padded)) as { exp?: number };
    return typeof json.exp === "number" ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

/** True when a present access token expires within `withinMs` (or is undecodable). */
export function isAccessTokenExpiringSoon(token: string, withinMs = 90_000): boolean {
  const exp = getAccessTokenExpiresAt(token);
  if (exp == null) return true;
  return exp - Date.now() <= withinMs;
}
