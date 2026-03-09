const rawBase = import.meta.env.VITE_API_URL || '';
const normalizedBase = rawBase.endsWith('/') ? rawBase.slice(0, -1) : rawBase;
const envBase = normalizedBase.endsWith('/api') ? normalizedBase.slice(0, -4) : normalizedBase;

const isExternalHost = window.location.hostname.endsWith('diegobarrioh.dev');
const defaultBase = isExternalHost ? window.location.origin : `http://${window.location.hostname}:8080`;
export const apiBase = envBase || defaultBase;

type RequestOptions = RequestInit & { timeoutMs?: number };

function mergeSignal(existing?: AbortSignal, timeoutMs?: number) {
  if (!timeoutMs) return { signal: existing, clear: () => {} };
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  let signal = controller.signal;
  if (existing && typeof (AbortSignal as any).any === 'function') {
    signal = (AbortSignal as any).any([existing, controller.signal]);
  }
  return { signal, clear: () => window.clearTimeout(timeoutId) };
}

export async function apiJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs, ...fetchOptions } = options;
  const { signal, clear } = mergeSignal(fetchOptions.signal ?? undefined, timeoutMs);
  try {
    const res = await fetch(url, { ...fetchOptions, signal });
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
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      const timeoutErr: any = new Error('timeout');
      timeoutErr.code = 'timeout';
      throw timeoutErr;
    }
    throw err;
  } finally {
    clear();
  }
}

// RAG module API functions
import type { SourceDocument, GeneratedDraft, GenerateQuizCommand, GenerateQuizResponse } from './types';

export async function uploadSource(token: string, file: File): Promise<SourceDocument> {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${apiBase}/api/sources`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form
  });
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    err.body = await res.json().catch(() => ({}));
    throw err;
  }
  return res.json();
}

export async function getSources(token: string): Promise<SourceDocument[]> {
  return apiAuthJson<SourceDocument[]>(`${apiBase}/api/sources`, token);
}

export async function generateQuiz(token: string, cmd: GenerateQuizCommand): Promise<GenerateQuizResponse> {
  return apiAuthJson<GenerateQuizResponse>(`${apiBase}/api/ai/quizzes/generate`, token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd),
    timeoutMs: 120_000
  });
}

export async function getDrafts(token: string, sourceId: string): Promise<GeneratedDraft[]> {
  return apiAuthJson<GeneratedDraft[]>(`${apiBase}/api/ai/quizzes/drafts?sourceId=${sourceId}`, token);
}

export async function getUnitsForSubject(token: string, subjectId: string): Promise<{ id: string; name: string }[]> {
  return apiAuthJson<{ id: string; name: string }[]>(`${apiBase}/api/subjects/${subjectId}/units`, token);
}

export async function apiAuthJson<T>(url: string, token: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs, ...fetchOptions } = options;
  const { signal, clear } = mergeSignal(fetchOptions.signal ?? undefined, timeoutMs);
  try {
    const res = await fetch(url, {
      ...fetchOptions,
      signal,
      headers: { ...(fetchOptions.headers || {}), Authorization: `Bearer ${token}` }
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
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      const timeoutErr: any = new Error('timeout');
      timeoutErr.code = 'timeout';
      throw timeoutErr;
    }
    throw err;
  } finally {
    clear();
  }
}
