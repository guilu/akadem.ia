const rawBase = import.meta.env.VITE_API_URL || window.location.origin;
const normalizedBase = rawBase.endsWith('/') ? rawBase.slice(0, -1) : rawBase;
export const apiBase = normalizedBase.endsWith('/api') ? normalizedBase.slice(0, -4) : normalizedBase;

export async function apiJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, options);
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    err.body = await res.json().catch(() => ({}));
    throw err;
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
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
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}
