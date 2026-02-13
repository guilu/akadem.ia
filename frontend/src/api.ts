export const apiBase = import.meta.env.VITE_API_URL || `http://${window.location.hostname}:8080`;

export async function apiJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, options);
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    err.body = await res.json().catch(() => ({}));
    throw err;
  }
  return res.json() as Promise<T>;
}

export async function apiAuthJson<T>(url: string, token: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: { ...(options.headers || {}), Authorization: `Bearer ${token}` }
  });
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    err.body = await res.json().catch(() => ({}));
    throw err;
  }
  return res.json() as Promise<T>;
}
