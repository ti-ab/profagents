
export async function getBearerToken(): Promise<string | null> {
  try {
    const res = await fetch('/api/auth/token', { cache: 'no-store' });
    if (!res.ok) return null;
    const token = await res.json();
    // try common fields
    return token?.accessToken || token?.idToken || token?.id_token || null;
  } catch {
    return null;
  }
}
